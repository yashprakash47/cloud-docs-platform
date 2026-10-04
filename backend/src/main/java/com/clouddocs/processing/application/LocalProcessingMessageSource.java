package com.clouddocs.processing.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@ConditionalOnProperty(name = "clouddocs.processing.message-source", havingValue = "local", matchIfMissing = true)
public class LocalProcessingMessageSource implements ProcessingMessageSource {
    private final Queue<ProcessingMessage> messages = new ConcurrentLinkedQueue<>();

    public void submit(ProcessingMessage message) {
        messages.add(message);
    }

    @Override
    public Optional<ProcessingMessageEnvelope> receive() {
        return Optional.ofNullable(messages.poll())
                .map(message -> new ProcessingMessageEnvelope(message, () -> { }));
    }
}
