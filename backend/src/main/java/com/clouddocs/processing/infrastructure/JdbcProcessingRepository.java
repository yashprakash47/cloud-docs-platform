package com.clouddocs.processing.infrastructure;

import com.clouddocs.operations.api.ResourceNotFoundException;
import com.clouddocs.processing.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProcessingRepository {
    private final JdbcTemplate jdbc;

    public JdbcProcessingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProcessingJob> findByMessageId(String messageId) {
        return jdbc.query("SELECT * FROM processing_jobs WHERE message_id=?", this::mapJob, messageId)
                .stream().findFirst();
    }

    public ProcessingJob insertQueued(UUID tenantId, UUID documentId, UUID versionId, String messageId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO processing_jobs(id, tenant_id, document_id, document_version_id, status, message_id)
                VALUES (?, ?, ?, ?, 'QUEUED', ?)
                ON CONFLICT (message_id) DO NOTHING
                """, id, tenantId, documentId, versionId, messageId);
        return findByMessageId(messageId).orElseThrow(() -> new IllegalStateException("Processing job was not created"));
    }

    public ProcessingJob findByIdAndTenant(UUID tenantId, UUID jobId) {
        return jdbc.query("SELECT * FROM processing_jobs WHERE tenant_id=? AND id=?", this::mapJob, tenantId, jobId)
                .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Processing job not found in this tenant"));
    }

    public List<ProcessingJob> findByDocument(UUID tenantId, UUID documentId) {
        return jdbc.query("SELECT * FROM processing_jobs WHERE tenant_id=? AND document_id=? ORDER BY created_at DESC",
                this::mapJob, tenantId, documentId);
    }

    public int markRunning(UUID tenantId, UUID jobId) {
        return jdbc.update("""
                UPDATE processing_jobs
                SET status='RUNNING', started_at=COALESCE(started_at, CURRENT_TIMESTAMP), attempt_count=attempt_count+1
                WHERE tenant_id=? AND id=? AND status='QUEUED'
                """, tenantId, jobId);
    }

    public ProcessingAttempt insertAttempt(UUID tenantId, UUID jobId, int attemptNumber) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO processing_attempts(id, tenant_id, processing_job_id, attempt_number, started_at, status) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'RUNNING')",
                id, tenantId, jobId, attemptNumber);
        return findAttempt(tenantId, jobId, attemptNumber);
    }

    public int markSucceeded(UUID tenantId, UUID jobId, int attemptNumber) {
        int updated = jdbc.update("UPDATE processing_jobs SET status='SUCCEEDED', completed_at=CURRENT_TIMESTAMP, failure_reason=NULL WHERE tenant_id=? AND id=? AND status='RUNNING'",
                tenantId, jobId);
        if (updated == 1) {
            jdbc.update("UPDATE processing_attempts SET status='SUCCEEDED', ended_at=CURRENT_TIMESTAMP, error_message=NULL WHERE tenant_id=? AND processing_job_id=? AND attempt_number=? AND status='RUNNING'",
                    tenantId, jobId, attemptNumber);
        }
        return updated;
    }

    public int markFailed(UUID tenantId, UUID jobId, int attemptNumber, String reason) {
        int updated = jdbc.update("UPDATE processing_jobs SET status='FAILED', completed_at=CURRENT_TIMESTAMP, failure_reason=? WHERE tenant_id=? AND id=? AND status='RUNNING'",
                reason, tenantId, jobId);
        if (updated == 1) {
            jdbc.update("UPDATE processing_attempts SET status='FAILED', ended_at=CURRENT_TIMESTAMP, error_message=? WHERE tenant_id=? AND processing_job_id=? AND attempt_number=? AND status='RUNNING'",
                    reason, tenantId, jobId, attemptNumber);
        }
        return updated;
    }

    public int markQueuedForRetry(UUID tenantId, UUID jobId) {
        return jdbc.update("UPDATE processing_jobs SET status='QUEUED', completed_at=NULL, failure_reason=NULL WHERE tenant_id=? AND id=? AND status='FAILED'",
                tenantId, jobId);
    }

    public List<ProcessingAttempt> findAttempts(UUID tenantId, UUID jobId) {
        return jdbc.query("SELECT * FROM processing_attempts WHERE tenant_id=? AND processing_job_id=? ORDER BY attempt_number",
                this::mapAttempt, tenantId, jobId);
    }

    private ProcessingAttempt findAttempt(UUID tenantId, UUID jobId, int number) {
        return jdbc.query("SELECT * FROM processing_attempts WHERE tenant_id=? AND processing_job_id=? AND attempt_number=?",
                this::mapAttempt, tenantId, jobId, number).stream().findFirst().orElseThrow();
    }

    private ProcessingJob mapJob(ResultSet rs, int row) throws SQLException {
        return new ProcessingJob(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getObject("document_id", UUID.class), rs.getObject("document_version_id", UUID.class),
                ProcessingJobStatus.valueOf(rs.getString("status")), instant(rs, "created_at"), instant(rs, "started_at"),
                instant(rs, "completed_at"), rs.getString("failure_reason"), rs.getInt("attempt_count"), rs.getString("message_id"));
    }

    private ProcessingAttempt mapAttempt(ResultSet rs, int row) throws SQLException {
        return new ProcessingAttempt(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class),
                rs.getObject("processing_job_id", UUID.class), rs.getInt("attempt_number"), instant(rs, "started_at"),
                instant(rs, "ended_at"), ProcessingAttemptStatus.valueOf(rs.getString("status")), rs.getString("error_message"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
