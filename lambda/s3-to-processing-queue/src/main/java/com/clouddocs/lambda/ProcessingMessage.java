package com.clouddocs.lambda;

public record ProcessingMessage(
        String messageId,
        String eventType,
        String bucket,
        String objectKey,
        String s3EventName,
        String versionId,
        String eTag,
        Long size,
        String eventTime,
        String source) {
}
