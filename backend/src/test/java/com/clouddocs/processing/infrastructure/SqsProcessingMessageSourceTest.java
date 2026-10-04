package com.clouddocs.processing.infrastructure;

import com.clouddocs.processing.application.ProcessingMessageEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SqsProcessingMessageSourceTest {
    @Test
    void mapsMessageAndAcknowledgesWithReceiptHandle() throws Exception {
        SqsClient sqs = mock(SqsClient.class);
        SqsProcessingProperties properties = properties();
        String body = """
                {"messageId":"event-1","eventType":"DOCUMENT_CREATED","bucket":"bucket",
                 "objectKey":"tenants/t/documents/d/versions/v/content","source":"s3"}
                """;
        Message message = Message.builder().messageId("sqs-1").receiptHandle("receipt-1").body(body).build();
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        SqsProcessingMessageSource source = new SqsProcessingMessageSource(sqs, new ObjectMapper(), properties);
        ProcessingMessageEnvelope envelope = source.receive().orElseThrow();

        assertEquals("event-1", envelope.message().messageId());
        envelope.acknowledge();
        verify(sqs).deleteMessage(DeleteMessageRequest.builder().queueUrl(properties.getQueueUrl()).receiptHandle("receipt-1").build());
        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqs).receiveMessage(captor.capture());
        ReceiveMessageRequest request = captor.getValue();
        assertEquals(20, request.waitTimeSeconds());
        assertEquals(10, request.maxNumberOfMessages());
        assertEquals(120, request.visibilityTimeout());
    }

    @Test
    void rejectsMalformedMessageWithoutAcknowledging() {
        SqsClient sqs = mock(SqsClient.class);
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("sqs-1").receiptHandle("receipt-1").body("{bad-json").build()).build());

        SqsProcessingMessageSource source = new SqsProcessingMessageSource(sqs, new ObjectMapper(), properties());

        assertThrows(IllegalArgumentException.class, source::receive);
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void rejectsMissingRequiredFields() {
        SqsClient sqs = mock(SqsClient.class);
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("sqs-1").receiptHandle("receipt-1").body("{\"messageId\":\"event-1\"}").build()).build());

        SqsProcessingMessageSource source = new SqsProcessingMessageSource(sqs, new ObjectMapper(), properties());

        assertThrows(IllegalArgumentException.class, source::receive);
    }

    private SqsProcessingProperties properties() {
        SqsProcessingProperties properties = new SqsProcessingProperties();
        properties.setQueueUrl("https://sqs.ap-south-1.amazonaws.com/123/clouddocs-processing");
        properties.setRegion("ap-south-1");
        return properties;
    }
}
