package com.clouddocs.processing.infrastructure;

import com.clouddocs.processing.application.ProcessingMessageEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "CLOUDDOCS_SQS_INTEGRATION_TEST", matches = "true")
class SqsProcessingMessageSourceIntegrationTest {
    @Test
    void sendsReceivesAndAcknowledgesAnIsolatedTestMessage() throws Exception {
        String queueUrl = required("CLOUDDOCS_PROCESSING_QUEUE_URL");
        String region = System.getenv().getOrDefault("AWS_REGION", "ap-south-1");
        String messageId = "clouddocs-sqs-integration-" + UUID.randomUUID();
        String body = "{\"messageId\":\"" + messageId + "\",\"eventType\":\"INTEGRATION_TEST\","
                + "\"bucket\":\"integration-test\",\"objectKey\":\"integration-tests/" + messageId + "\",\"source\":\"test\"}";

        try (SqsClient client = SqsClient.builder().region(Region.of(region)).build()) {
            client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build());
            SqsProcessingProperties properties = new SqsProcessingProperties();
            properties.setQueueUrl(queueUrl);
            properties.setRegion(region);
            properties.setWaitTimeSeconds(20);
            properties.setMaxMessages(1);
            properties.setVisibilityTimeoutSeconds(120);
            try (SqsProcessingMessageSource source = new SqsProcessingMessageSource(client, new ObjectMapper(), properties)) {
                ProcessingMessageEnvelope envelope = source.receive().orElseThrow();
                assertEquals(messageId, envelope.message().messageId());
                envelope.acknowledge();
            }
        }
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
