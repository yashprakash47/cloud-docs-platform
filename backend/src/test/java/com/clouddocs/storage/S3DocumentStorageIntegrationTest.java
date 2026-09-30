package com.clouddocs.storage;

import com.clouddocs.storage.infrastructure.S3DocumentStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "clouddocs.storage.provider=s3")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "CLOUDDOCS_S3_INTEGRATION_TEST", matches = "true")
class S3DocumentStorageIntegrationTest {
    @Autowired S3DocumentStorage storage;

    @Test
    void uploadsDownloadsAndDeletesAnIsolatedTestObject() throws Exception {
        String key = "integration-tests/" + UUID.randomUUID() + "/content";
        byte[] content = "CloudDocs S3 integration test".getBytes(StandardCharsets.UTF_8);
        try {
            var stored = storage.upload(key, new ByteArrayInputStream(content), content.length, "text/plain");
            assertTrue(storage.exists(key));
            assertArrayEquals(content, storage.download(key).readAllBytes());
            assertEquals("text/plain", stored.contentType());
        } finally {
            storage.delete(key);
            assertFalse(storage.exists(key));
        }
    }
}
