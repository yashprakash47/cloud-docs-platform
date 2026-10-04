package com.clouddocs.processing.application;

public record ProcessingMessage(
        String messageId,
        String eventType,
        String bucket,
        String objectKey,
        String versionId,
        String eTag,
        Long size,
        String eventTime,
        String source) {
}
