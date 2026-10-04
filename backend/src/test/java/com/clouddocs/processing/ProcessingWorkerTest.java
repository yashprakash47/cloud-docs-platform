package com.clouddocs.processing;

import com.clouddocs.processing.application.*;
import com.clouddocs.processing.domain.ProcessingJob;
import com.clouddocs.processing.domain.ProcessingJobStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessingWorkerTest {
    @Test
    void successfulProcessingAcknowledgesMessage() {
        ProcessingMessageSource source = mock(ProcessingMessageSource.class);
        ProcessingService service = mock(ProcessingService.class);
        AtomicBoolean acknowledged = new AtomicBoolean();
        ProcessingMessage message = message("event-1");
        ProcessingJob job = job(ProcessingJobStatus.SUCCEEDED);
        when(source.receive()).thenReturn(Optional.of(new ProcessingMessageEnvelope(message, () -> acknowledged.set(true))));
        when(service.process(message)).thenReturn(job);

        assertEquals(Optional.of(job), new ProcessingWorker(source, service).processNext());
        assertTrue(acknowledged.get());
    }

    @Test
    void failedProcessingLeavesMessageUnacknowledged() {
        ProcessingMessageSource source = mock(ProcessingMessageSource.class);
        ProcessingService service = mock(ProcessingService.class);
        AtomicBoolean acknowledged = new AtomicBoolean();
        ProcessingMessage message = message("event-1");
        when(source.receive()).thenReturn(Optional.of(new ProcessingMessageEnvelope(message, () -> acknowledged.set(true))));
        when(service.process(message)).thenReturn(job(ProcessingJobStatus.FAILED));

        new ProcessingWorker(source, service).processNext();
        assertFalse(acknowledged.get());
    }

    private ProcessingMessage message(String id) {
        return new ProcessingMessage(id, "DOCUMENT_CREATED", "bucket", "object-key", null, null, 1L, null, "s3");
    }

    private ProcessingJob job(ProcessingJobStatus status) {
        return new ProcessingJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status,
                Instant.now(), null, null, null, 0, "event-1");
    }
}
