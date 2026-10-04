package com.clouddocs.processing.infrastructure;

import com.clouddocs.processing.domain.ProcessingResult;
import com.clouddocs.storage.application.StorageObjectKeyGenerator;
import com.clouddocs.storage.infrastructure.S3DocumentStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in verification of the deployed S3 -> Lambda -> SQS path and the local
 * Spring processing worker. The database is an isolated in-memory test database;
 * no production document records are created or changed.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "CLOUDDOCS_SQS_INTEGRATION_TEST", matches = "true")
class SqsProcessingEndToEndIntegrationTest {
    private static final String BUCKET = "clouddocs-documents-315527";

    @Autowired JdbcTemplate jdbc;
    @Autowired S3DocumentStorage storage;

    private UUID tenantId;
    private UUID documentId;
    private UUID versionId;
    private String objectKey;

    @DynamicPropertySource
    static void awsProperties(DynamicPropertyRegistry registry) {
        registry.add("clouddocs.processing.message-source", () -> "sqs");
        registry.add("clouddocs.processing.sqs.queue-url", () -> required("CLOUDDOCS_PROCESSING_QUEUE_URL"));
        registry.add("clouddocs.processing.sqs.region", () -> envOrDefault("AWS_REGION", "ap-south-1"));
        registry.add("clouddocs.processing.sqs.wait-time-seconds", () -> 20);
        registry.add("clouddocs.processing.sqs.max-messages", () -> 10);
        registry.add("clouddocs.processing.sqs.visibility-timeout-seconds", () -> 120);
        registry.add("clouddocs.storage.provider", () -> "s3");
        registry.add("clouddocs.storage.s3.bucket", () -> envOrDefault("CLOUDDOCS_S3_BUCKET", BUCKET));
        registry.add("clouddocs.storage.s3.region", () -> envOrDefault("AWS_REGION", "ap-south-1"));
    }

    @Test
    void realS3UploadIsProcessedAndMessageIsAcknowledged() throws Exception {
        tenantId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        String testId = UUID.randomUUID().toString();
        objectKey = StorageObjectKeyGenerator.documentVersion(tenantId, documentId, versionId);
        byte[] content = ("CloudDocs SQS processing integration test " + testId).getBytes(StandardCharsets.UTF_8);
        String checksum = sha256(content);
        String email = "sqs-integration-" + testId + "@example.test";

        seedIsolatedDocument(userId, membershipId, email, checksum, content.length);

        try {
            storage.upload(objectKey, new ByteArrayInputStream(content), content.length, "text/plain");
            assertTrue(storage.exists(objectKey));
            assertArrayEquals(content, storage.download(objectKey).readAllBytes());

            ProcessingResult result = awaitSucceeded();

            assertEquals(content.length, result.sizeBytes());
            assertEquals(checksum, result.sha256());
            assertTrue(result.checksumVerified());
            assertEquals(1, jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM processing_attempts a
                    JOIN processing_jobs j ON j.tenant_id=a.tenant_id AND j.id=a.processing_job_id
                    WHERE a.tenant_id=? AND j.document_id=? AND a.status='SUCCEEDED'
                    """,
                    Integer.class, tenantId, documentId));
            assertTargetMessageIsAbsentFromQueue(objectKey);
        } finally {
            if (objectKey != null) storage.delete(objectKey);
            cleanupIsolatedRecords(userId, membershipId);
        }
    }

    private ProcessingResult awaitSucceeded() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 180_000;
        while (System.currentTimeMillis() < deadline) {
            var rows = jdbc.query("SELECT status, failure_reason FROM processing_jobs WHERE tenant_id=? AND document_id=?",
                    (rs, row) -> new JobState(rs.getString("status"), rs.getString("failure_reason")), tenantId, documentId);
            if (!rows.isEmpty()) {
                JobState state = rows.get(0);
                if ("SUCCEEDED".equals(state.status())) {
                    return new ProcessingResult(
                            jdbc.queryForObject("SELECT size_bytes FROM document_versions WHERE id=?", Long.class, versionId),
                            jdbc.queryForObject("SELECT checksum FROM document_versions WHERE id=?", String.class, versionId),
                            true);
                }
                if ("FAILED".equals(state.status())) {
                    fail("Processing job failed: " + state.failureReason());
                }
            }
            Thread.sleep(1_000);
        }
        fail("Timed out waiting for the SQS processing job to reach SUCCEEDED");
        return null;
    }

    private void assertTargetMessageIsAbsentFromQueue(String key) {
        String queueUrl = required("CLOUDDOCS_PROCESSING_QUEUE_URL");
        try (SqsClient sqs = SqsClient.builder().region(Region.of(envOrDefault("AWS_REGION", "ap-south-1"))).build()) {
            var response = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl).waitTimeSeconds(1).maxNumberOfMessages(10).visibilityTimeout(5).build());
            assertTrue(response.messages().stream().noneMatch(message -> message.body().contains(key)),
                    "The successfully processed SQS message was still available");
        }
    }

    private void seedIsolatedDocument(UUID userId, UUID membershipId, String email, String checksum, long size) {
        jdbc.update("INSERT INTO users(id,email,display_name,status) VALUES (?,?,?,'ACTIVE')",
                userId, email, "SQS Integration Test");
        jdbc.update("INSERT INTO tenants(id,name) VALUES (?,?)", tenantId, "SQS Integration Test " + tenantId);
        jdbc.update("INSERT INTO memberships(id,user_id,tenant_id,role) VALUES (?,?,?,'TENANT_ADMIN')",
                membershipId, userId, tenantId);
        jdbc.update("INSERT INTO documents(id,tenant_id,owner_user_id,name,status) VALUES (?,?,?,?,'ACTIVE')",
                documentId, tenantId, userId, "SQS integration document");
        jdbc.update("""
                INSERT INTO document_versions(id,tenant_id,document_id,version_number,storage_reference,original_filename,content_type,size_bytes,checksum,uploaded_by)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, versionId, tenantId, documentId, 1, objectKey, "sqs-integration.txt", "text/plain", size, checksum, userId);
        jdbc.update("UPDATE documents SET current_version_id=? WHERE tenant_id=? AND id=?",
                versionId, tenantId, documentId);
    }

    private void cleanupIsolatedRecords(UUID userId, UUID membershipId) {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM processing_attempts WHERE tenant_id=?", tenantId);
        jdbc.update("DELETE FROM processing_jobs WHERE tenant_id=?", tenantId);
        jdbc.update("UPDATE documents SET current_version_id=NULL WHERE tenant_id=?", tenantId);
        jdbc.update("DELETE FROM document_versions WHERE tenant_id=?", tenantId);
        jdbc.update("DELETE FROM documents WHERE tenant_id=?", tenantId);
        jdbc.update("DELETE FROM memberships WHERE id=?", membershipId);
        jdbc.update("DELETE FROM tenants WHERE id=?", tenantId);
        jdbc.update("DELETE FROM users WHERE id=?", userId);
    }

    @AfterEach
    void resetState() {
        tenantId = null;
        documentId = null;
        versionId = null;
        objectKey = null;
    }

    private static String sha256(byte[] content) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record JobState(String status, String failureReason) { }
}
