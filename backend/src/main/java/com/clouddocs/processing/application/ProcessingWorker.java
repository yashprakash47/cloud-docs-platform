package com.clouddocs.processing.application;

import com.clouddocs.processing.domain.ProcessingJob;
import com.clouddocs.processing.domain.ProcessingJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessingWorker {
    private static final Logger log = LoggerFactory.getLogger(ProcessingWorker.class);
    private final ProcessingMessageSource source;
    private final ProcessingService processing;

    public ProcessingWorker(ProcessingMessageSource source, ProcessingService processing) {
        this.source = source;
        this.processing = processing;
    }

    public Optional<ProcessingJob> processNext() {
        return source.receive().map(this::processOne);
    }

    private ProcessingJob processOne(ProcessingMessageEnvelope envelope) {
        ProcessingJob job = processing.process(envelope.message());
        log.info("Processing message {} produced job {} with status {}", envelope.message().messageId(), job.id(), job.status());
        if (job.status() == ProcessingJobStatus.SUCCEEDED) {
            try {
                envelope.acknowledge();
            } catch (Exception exception) {
                throw new IllegalStateException("Processing message acknowledgement failed", exception);
            }
        }
        return job;
    }
}
