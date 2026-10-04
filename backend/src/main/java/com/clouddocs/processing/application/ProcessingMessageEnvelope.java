package com.clouddocs.processing.application;

public record ProcessingMessageEnvelope(ProcessingMessage message, Acknowledgement acknowledgement) {
    public void acknowledge() throws Exception {
        acknowledgement.acknowledge();
    }

    @FunctionalInterface
    public interface Acknowledgement {
        void acknowledge() throws Exception;
    }
}
