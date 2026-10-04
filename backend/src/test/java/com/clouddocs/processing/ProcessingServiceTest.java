package com.clouddocs.processing;

import com.clouddocs.document.application.DocumentService;
import com.clouddocs.document.domain.DocumentVersion;
import com.clouddocs.identity.domain.User;
import com.clouddocs.identity.domain.UserStatus;
import com.clouddocs.operations.api.ForbiddenOperationException;
import com.clouddocs.operations.api.InvalidRequestException;
import com.clouddocs.processing.application.*;
import com.clouddocs.processing.domain.*;
import com.clouddocs.processing.infrastructure.JdbcProcessingRepository;
import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.tenancy.application.TenantAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID DOCUMENT = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final String KEY = "tenants/%s/documents/%s/versions/%s/content".formatted(TENANT, DOCUMENT, VERSION);
    private static final String MESSAGE_ID = "message-1";

    @Mock JdbcProcessingRepository repository;
    @Mock DocumentService documents;
    @Mock TenantAuthorizationService authorization;
    @Mock DocumentStorage storage;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ProcessingService service;

    @Test
    void queuesAndDeduplicatesByMessageId() {
        ProcessingJob job = job(ProcessingJobStatus.QUEUED, 0);
        ProcessingMessage message = message(MESSAGE_ID);
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty(), Optional.of(job));
        when(repository.insertQueued(TENANT, DOCUMENT, VERSION, MESSAGE_ID)).thenReturn(job);

        assertSame(job, service.queue(message));
        assertSame(job, service.queue(message));
        verify(repository, times(1)).insertQueued(TENANT, DOCUMENT, VERSION, MESSAGE_ID);
    }

    @Test
    void rejectsInvalidStateTransition() {
        ProcessingJob running = job(ProcessingJobStatus.RUNNING, 1);
        when(repository.findByIdAndTenant(TENANT, running.id())).thenReturn(running);

        assertThrows(InvalidRequestException.class, () -> service.start(TENANT, running.id()));
        verify(repository, never()).markRunning(any(), any());
    }

    @Test
    void retriesFailedJobAndEnforcesAuthorization() {
        ProcessingJob failed = job(ProcessingJobStatus.FAILED, 1);
        ProcessingJob queued = job(ProcessingJobStatus.QUEUED, 1);
        User member = new User(UUID.randomUUID(), "member@example.com", "Member", UserStatus.ACTIVE, Instant.now(), Instant.now());
        when(repository.findByIdAndTenant(TENANT, failed.id())).thenReturn(failed, queued);
        when(repository.markQueuedForRetry(TENANT, failed.id())).thenReturn(1);

        assertSame(queued, service.retry(member, TENANT, failed.id()));
        verify(authorization).requireWriteAccess(member, TENANT);

        doThrow(new ForbiddenOperationException("viewer cannot write")).when(authorization).requireWriteAccess(member, TENANT);
        assertThrows(ForbiddenOperationException.class, () -> service.retry(member, TENANT, failed.id()));
    }

    @Test
    void readsJobsOnlyThroughTheRequestedTenant() {
        User member = new User(UUID.randomUUID(), "member@example.com", "Member", UserStatus.ACTIVE, Instant.now(), Instant.now());
        ProcessingJob job = job(ProcessingJobStatus.SUCCEEDED, 1);
        when(repository.findByIdAndTenant(TENANT, job.id())).thenReturn(job);

        assertSame(job, service.get(member, TENANT, job.id()));
        verify(authorization).requireMembership(member, TENANT);
        verify(repository).findByIdAndTenant(TENANT, job.id());
    }

    @Test
    void processesExistingObjectAndTracksAttempt() throws IOException {
        ProcessingJob queued = job(ProcessingJobStatus.QUEUED, 0);
        ProcessingJob running = job(queued.id(), ProcessingJobStatus.RUNNING, 1);
        ProcessingJob succeeded = job(queued.id(), ProcessingJobStatus.SUCCEEDED, 1);
        DocumentVersion version = new DocumentVersion(VERSION, TENANT, DOCUMENT, 1, KEY, "a.txt", "text/plain", 5, null, UUID.randomUUID(), Instant.now());
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty());
        when(repository.insertQueued(TENANT, DOCUMENT, VERSION, MESSAGE_ID)).thenReturn(queued);
        when(repository.findByIdAndTenant(TENANT, queued.id())).thenReturn(queued, running, running, running, succeeded);
        when(repository.markRunning(TENANT, queued.id())).thenReturn(1);
        when(repository.insertAttempt(TENANT, queued.id(), 1)).thenReturn(null);
        when(repository.markSucceeded(TENANT, queued.id(), 1)).thenReturn(1);
        when(documents.requireVersion(TENANT, DOCUMENT, VERSION)).thenReturn(version);
        when(storage.exists(KEY)).thenReturn(true);
        when(storage.download(KEY)).thenReturn(new ByteArrayInputStream("hello".getBytes()));

        ProcessingJob result = service.process(message(MESSAGE_ID));

        assertEquals(ProcessingJobStatus.SUCCEEDED, result.status());
        verify(repository).insertAttempt(TENANT, queued.id(), 1);
        verify(events).publishEvent(any(ProcessingCompletedEvent.class));
    }

    @Test
    void missingObjectFailsJobAndEmitsFailureEvent() throws IOException {
        ProcessingJob queued = job(ProcessingJobStatus.QUEUED, 0);
        ProcessingJob running = job(queued.id(), ProcessingJobStatus.RUNNING, 1);
        ProcessingJob failed = job(queued.id(), ProcessingJobStatus.FAILED, 1);
        DocumentVersion version = new DocumentVersion(VERSION, TENANT, DOCUMENT, 1, KEY, "a.txt", "text/plain", 5, null, UUID.randomUUID(), Instant.now());
        when(repository.findByMessageId(MESSAGE_ID)).thenReturn(Optional.empty());
        when(repository.insertQueued(TENANT, DOCUMENT, VERSION, MESSAGE_ID)).thenReturn(queued);
        when(repository.findByIdAndTenant(TENANT, queued.id())).thenReturn(queued, running, running, running, failed);
        when(repository.markRunning(TENANT, queued.id())).thenReturn(1);
        when(repository.insertAttempt(TENANT, queued.id(), 1)).thenReturn(null);
        when(repository.markFailed(eq(TENANT), eq(queued.id()), eq(1), anyString())).thenReturn(1);
        when(documents.requireVersion(TENANT, DOCUMENT, VERSION)).thenReturn(version);
        when(storage.exists(KEY)).thenReturn(false);

        assertEquals(ProcessingJobStatus.FAILED, service.process(message(MESSAGE_ID)).status());
        verify(events).publishEvent(any(ProcessingFailedEvent.class));
    }

    private ProcessingMessage message(String id) {
        return new ProcessingMessage(id, "DOCUMENT_CREATED", "bucket", KEY, null, null, 5L, null, "local");
    }

    private ProcessingJob job(ProcessingJobStatus status, int attempts) {
        return job(UUID.randomUUID(), status, attempts);
    }

    private ProcessingJob job(UUID id, ProcessingJobStatus status, int attempts) {
        return new ProcessingJob(id, TENANT, DOCUMENT, VERSION, status, Instant.now(),
                status == ProcessingJobStatus.QUEUED ? null : Instant.now(),
                status == ProcessingJobStatus.QUEUED || status == ProcessingJobStatus.RUNNING ? null : Instant.now(),
                status == ProcessingJobStatus.FAILED ? "failed" : null, attempts, MESSAGE_ID);
    }
}
