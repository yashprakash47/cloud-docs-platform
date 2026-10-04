package com.clouddocs.processing.application;

import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LocalProcessingMessageSource implements ProcessingMessageSource {
    private final Queue<ProcessingMessage> messages = new ConcurrentLinkedQueue<>();

    public void submit(ProcessingMessage message) {
        messages.add(message);
    }

    @Override
    public Optional<ProcessingMessage> receive() {
        return Optional.ofNullable(messages.poll());
    }
}
