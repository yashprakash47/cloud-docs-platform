package com.clouddocs.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class S3ToProcessingQueueHandler implements RequestHandler<S3Event, String> {
    private static final Logger LOGGER = Logger.getLogger(S3ToProcessingQueueHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SqsClient sqs;
    private final String queueUrl;

    public S3ToProcessingQueueHandler() {
        this(SqsClient.create(), requiredEnvironment("CLOUDDOCS_PROCESSING_QUEUE_URL"));
    }

    S3ToProcessingQueueHandler(SqsClient sqs, String queueUrl) {
        this.sqs = Objects.requireNonNull(sqs, "sqs");
        this.queueUrl = Objects.requireNonNull(queueUrl, "queueUrl");
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        if (event == null || event.getRecords() == null) throw new IllegalArgumentException("S3 event is required");
        int published = 0;
        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            ProcessingMessage message = toMessage(record);
            String body = serialize(message);
            try {
                sqs.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build());
                published++;
            } catch (RuntimeException exception) {
                LOGGER.log(Level.SEVERE, "Failed to publish S3 processing message " + message.messageId(), exception);
                throw exception;
            }
        }
        return Integer.toString(published);
    }

    private ProcessingMessage toMessage(S3EventNotification.S3EventNotificationRecord record) {
        if (record == null || record.getS3() == null || record.getS3().getBucket() == null || record.getS3().getObject() == null) {
            throw new IllegalArgumentException("S3 event record is missing bucket or object details");
        }
        String bucket = record.getS3().getBucket().getName();
        String key = record.getS3().getObject().getKey();
        String versionId = record.getS3().getObject().getVersionId();
        String eTag = record.getS3().getObject().geteTag();
        Long size = record.getS3().getObject().getSizeAsLong();
        String eventName = record.getEventName();
        String eventTime = record.getEventTime() == null ? null : record.getEventTime().toInstant().toString();
        String stableIdentity = String.join("|", nullToEmpty(eventName), nullToEmpty(bucket), nullToEmpty(key), nullToEmpty(versionId), nullToEmpty(eTag), nullToEmpty(eventTime));
        return new ProcessingMessage(hash(stableIdentity), "DOCUMENT_CREATED", bucket, key, eventName, versionId, eTag, size, eventTime, "s3");
    }

    private String serialize(ProcessingMessage message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize processing message", exception);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be configured");
        return value;
    }
}
