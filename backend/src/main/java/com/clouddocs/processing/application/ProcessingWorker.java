package com.clouddocs.processing.application;

import com.clouddocs.processing.domain.ProcessingJob;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessingWorker {
    private final ProcessingMessageSource source;
    private final ProcessingService processing;

    public ProcessingWorker(ProcessingMessageSource source, ProcessingService processing) {
        this.source = source;
        this.processing = processing;
    }

    public Optional<ProcessingJob> processNext() {
        return source.receive().map(processing::process);
    }
}
