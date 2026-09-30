package com.clouddocs.lambda;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3ToProcessingQueueHandlerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void publishesExplicitMessageForValidS3Record() throws Exception {
        SqsClient sqs = mock(SqsClient.class);
        S3ToProcessingQueueHandler handler = new S3ToProcessingQueueHandler(sqs, "https://sqs.ap-south-1.amazonaws.com/123/clouddocs-processing");
        assertEquals("1", handler.handleRequest(event(record("ObjectCreated:Put", "documents/a.txt", "v1", "etag-a", 123)), null));
        SendMessageRequest request = singleRequest(sqs);
        JsonNode message = JSON.readTree(request.messageBody());
        assertEquals("DOCUMENT_CREATED", message.get("eventType").asText());
        assertEquals("clouddocs-documents-315527", message.get("bucket").asText());
        assertEquals("documents/a.txt", message.get("objectKey").asText());
        assertEquals("ObjectCreated:Put", message.get("s3EventName").asText());
        assertEquals("v1", message.get("versionId").asText());
        assertEquals("etag-a", message.get("eTag").asText());
        assertEquals(123, message.get("size").asLong());
        assertEquals("s3", message.get("source").asText());
        assertTrue(message.get("messageId").asText().matches("[0-9a-f]{64}"));
        assertEquals("https://sqs.ap-south-1.amazonaws.com/123/clouddocs-processing", request.queueUrl());
    }

    @Test
    void publishesOneMessagePerS3Record() throws Exception {
        SqsClient sqs = mock(SqsClient.class);
        S3ToProcessingQueueHandler handler = new S3ToProcessingQueueHandler(sqs, "queue-url");
        assertEquals("2", handler.handleRequest(event(record("ObjectCreated:Put", "one", "v1", "e1", 1), record("ObjectCreated:CompleteMultipartUpload", "two", null, null, 2)), null));
        verify(sqs, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void serializesMissingOptionalFieldsAsNull() throws Exception {
        SqsClient sqs = mock(SqsClient.class);
        new S3ToProcessingQueueHandler(sqs, "queue-url").handleRequest(event(record("ObjectCreated:Put", "without-optionals", null, null, 0)), null);
        JsonNode message = JSON.readTree(singleRequest(sqs).messageBody());
        assertTrue(message.get("versionId").isNull());
        assertTrue(message.get("eTag").isNull());
    }

    @Test
    void rethrowsSqsFailure() {
        SqsClient sqs = mock(SqsClient.class);
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenThrow(SqsException.builder().message("queue unavailable").build());
        assertThrows(SqsException.class, () -> new S3ToProcessingQueueHandler(sqs, "queue-url").handleRequest(event(record("ObjectCreated:Put", "failed", null, null, 1)), null));
    }

    @Test
    void messageJsonContainsOnlyTheExplicitContract() throws Exception {
        SqsClient sqs = mock(SqsClient.class);
        new S3ToProcessingQueueHandler(sqs, "queue-url").handleRequest(event(record("ObjectCreated:Put", "contract", null, null, 7)), null);
        JsonNode message = JSON.readTree(singleRequest(sqs).messageBody());
        assertEquals(List.of("messageId", "eventType", "bucket", "objectKey", "s3EventName", "versionId", "eTag", "size", "eventTime", "source"), java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(message.fieldNames(), 0), false).toList());
        assertFalse(message.toString().contains("Records"));
    }

    private SendMessageRequest singleRequest(SqsClient sqs) {
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqs).sendMessage(captor.capture());
        return captor.getValue();
    }

    private S3Event event(S3EventNotification.S3EventNotificationRecord... records) {
        return new S3Event(List.of(records));
    }

    private S3EventNotification.S3EventNotificationRecord record(String eventName, String key, String versionId, String eTag, long size) {
        S3EventNotification.S3BucketEntity bucket = new S3EventNotification.S3BucketEntity(
                "clouddocs-documents-315527", null, null);
        S3EventNotification.S3ObjectEntity object = new S3EventNotification.S3ObjectEntity(
                key, size, eTag, versionId);
        S3EventNotification.S3Entity s3 = new S3EventNotification.S3Entity(
                null, bucket, object, null);
        return new S3EventNotification.S3EventNotificationRecord(
                "ap-south-1", eventName, "aws:s3", "2026-09-30T10:15:30.000Z", "2.1",
                null, null, s3, null);
    }
}
