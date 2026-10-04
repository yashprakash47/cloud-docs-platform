package com.clouddocs.processing.application;

import com.clouddocs.document.application.DocumentService;
import com.clouddocs.document.domain.DocumentVersion;
import com.clouddocs.identity.domain.User;
import com.clouddocs.operations.api.InvalidRequestException;
import com.clouddocs.processing.domain.*;
import com.clouddocs.processing.infrastructure.JdbcProcessingRepository;
import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.tenancy.application.TenantAuthorizationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcessingService {
    private static final Pattern OBJECT_KEY = Pattern.compile("^tenants/([^/]+)/documents/([^/]+)/versions/([^/]+)/content$");
    private static final int MAX_ATTEMPTS = 3;

    private final JdbcProcessingRepository repository;
    private final DocumentService documents;
    private final TenantAuthorizationService authorization;
    private final DocumentStorage storage;
    private final ApplicationEventPublisher events;

    public ProcessingService(JdbcProcessingRepository repository, DocumentService documents,
                             TenantAuthorizationService authorization, DocumentStorage storage,
                             ApplicationEventPublisher events) {
        this.repository = repository;
        this.documents = documents;
        this.authorization = authorization;
        this.storage = storage;
        this.events = events;
    }

    @Transactional
    public ProcessingJob queue(ProcessingMessage message) {
        if (message == null || message.messageId() == null || message.messageId().isBlank()) {
            throw new InvalidRequestException("A processing message id is required");
        }
        var existing = repository.findByMessageId(message.messageId());
        if (existing.isPresent()) return existing.get();
        DocumentReference reference = referenceFrom(message.objectKey());
        documents.requireVersion(reference.tenantId(), reference.documentId(), reference.versionId());
        return repository.insertQueued(reference.tenantId(), reference.documentId(), reference.versionId(), message.messageId());
    }

    @Transactional
    public ProcessingJob process(ProcessingMessage message) {
        ProcessingJob job = queue(message);
        if (job.status() != ProcessingJobStatus.QUEUED) return job;

        try {
            start(job.tenantId(), job.id());
        } catch (InvalidRequestException exception) {
            ProcessingJob current = repository.findByIdAndTenant(job.tenantId(), job.id());
            if (current.status() != ProcessingJobStatus.QUEUED) return current;
            throw exception;
        }
        job = repository.findByIdAndTenant(job.tenantId(), job.id());
        int attemptNumber = job.attemptCount();
        repository.insertAttempt(job.tenantId(), job.id(), attemptNumber);
        ProcessingResult result;
        try {
            DocumentVersion version = documents.requireVersion(job.tenantId(), job.documentId(), job.documentVersionId());
            result = verifyStoredObject(version);
        } catch (Exception exception) {
            fail(job.tenantId(), job.id(), attemptNumber, safeMessage(exception));
            return repository.findByIdAndTenant(job.tenantId(), job.id());
        }
        complete(job.tenantId(), job.id(), attemptNumber, result);
        return repository.findByIdAndTenant(job.tenantId(), job.id());
    }

    @Transactional
    public ProcessingJob start(UUID tenantId, UUID jobId) {
        ProcessingJob before = repository.findByIdAndTenant(tenantId, jobId);
        if (before.status() != ProcessingJobStatus.QUEUED) {
            throw new InvalidRequestException("Only queued processing jobs can be started");
        }
        if (repository.markRunning(tenantId, jobId) != 1) {
            throw new InvalidRequestException("Processing job could not be started");
        }
        return repository.findByIdAndTenant(tenantId, jobId);
    }

    @Transactional
    public ProcessingJob complete(UUID tenantId, UUID jobId, int attemptNumber, ProcessingResult result) {
        ProcessingJob before = repository.findByIdAndTenant(tenantId, jobId);
        if (before.status() != ProcessingJobStatus.RUNNING) {
            throw new InvalidRequestException("Only running processing jobs can be completed");
        }
        if (repository.markSucceeded(tenantId, jobId, attemptNumber) != 1) {
            throw new InvalidRequestException("Processing job could not be completed");
        }
        ProcessingJob completed = repository.findByIdAndTenant(tenantId, jobId);
        events.publishEvent(new ProcessingCompletedEvent(completed, result));
        return completed;
    }

    @Transactional
    public ProcessingJob fail(UUID tenantId, UUID jobId, int attemptNumber, String reason) {
        ProcessingJob before = repository.findByIdAndTenant(tenantId, jobId);
        if (before.status() != ProcessingJobStatus.RUNNING) {
            throw new InvalidRequestException("Only running processing jobs can fail");
        }
        if (repository.markFailed(tenantId, jobId, attemptNumber, reason) != 1) {
            throw new InvalidRequestException("Processing job could not be failed");
        }
        ProcessingJob failed = repository.findByIdAndTenant(tenantId, jobId);
        events.publishEvent(new ProcessingFailedEvent(failed, reason));
        return failed;
    }

    public ProcessingJob get(User user, UUID tenantId, UUID jobId) {
        authorization.requireMembership(user, tenantId);
        return repository.findByIdAndTenant(tenantId, jobId);
    }

    public List<ProcessingJob> listForDocument(User user, UUID tenantId, UUID documentId) {
        authorization.requireMembership(user, tenantId);
        documents.require(tenantId, documentId);
        return repository.findByDocument(tenantId, documentId);
    }

    @Transactional
    public ProcessingJob retry(User user, UUID tenantId, UUID jobId) {
        authorization.requireWriteAccess(user, tenantId);
        ProcessingJob job = repository.findByIdAndTenant(tenantId, jobId);
        if (job.status() != ProcessingJobStatus.FAILED) {
            throw new InvalidRequestException("Only failed processing jobs can be retried");
        }
        if (job.attemptCount() >= MAX_ATTEMPTS) {
            throw new InvalidRequestException("Processing job has reached the retry limit");
        }
        repository.markQueuedForRetry(tenantId, jobId);
        return repository.findByIdAndTenant(tenantId, jobId);
    }

    public List<ProcessingAttempt> attempts(UUID tenantId, UUID jobId) {
        return repository.findAttempts(tenantId, jobId);
    }

    private ProcessingResult verifyStoredObject(DocumentVersion version) throws IOException {
        if (!storage.exists(version.storageReference())) {
            throw new IOException("Referenced storage object does not exist");
        }
        MessageDigest digest = sha256();
        long size = 0;
        try (InputStream input = storage.download(version.storageReference())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                digest.update(buffer, 0, read);
            }
        }
        String checksum = hex(digest.digest());
        if (version.sizeBytes() != size) throw new IOException("Stored object size does not match document metadata");
        boolean verified = version.checksum() == null || version.checksum().equalsIgnoreCase(checksum);
        if (!verified) throw new IOException("Stored object checksum does not match document metadata");
        return new ProcessingResult(size, checksum, version.checksum() != null);
    }

    private DocumentReference referenceFrom(String key) {
        if (key == null) throw new InvalidRequestException("Processing message object key is required");
        Matcher matcher = OBJECT_KEY.matcher(key);
        if (!matcher.matches()) throw new InvalidRequestException("Processing object key has an invalid format");
        try {
            return new DocumentReference(UUID.fromString(matcher.group(1)), UUID.fromString(matcher.group(2)), UUID.fromString(matcher.group(3)));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Processing object key contains invalid identifiers");
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private record DocumentReference(UUID tenantId, UUID documentId, UUID versionId) { }
}
