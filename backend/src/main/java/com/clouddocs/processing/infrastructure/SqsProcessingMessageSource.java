package com.clouddocs.processing.infrastructure;

import com.clouddocs.processing.application.ProcessingMessage;
import com.clouddocs.processing.application.ProcessingMessageEnvelope;
import com.clouddocs.processing.application.ProcessingMessageSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "clouddocs.processing.message-source", havingValue = "sqs")
public class SqsProcessingMessageSource implements ProcessingMessageSource, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SqsProcessingMessageSource.class);

    private final SqsClient sqs;
    private final ObjectMapper objectMapper;
    private final SqsProcessingProperties properties;
    private final Queue<ProcessingMessageEnvelope> bufferedMessages = new ArrayDeque<>();

    @Autowired
    public SqsProcessingMessageSource(ObjectMapper objectMapper, SqsProcessingProperties properties) {
        properties.validate();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.sqs = SqsClient.builder().region(Region.of(properties.getRegion())).build();
    }

    SqsProcessingMessageSource(SqsClient sqs, ObjectMapper objectMapper, SqsProcessingProperties properties) {
        properties.validate();
        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public synchronized Optional<ProcessingMessageEnvelope> receive() {
        if (bufferedMessages.isEmpty()) fillBuffer();
        return Optional.ofNullable(bufferedMessages.poll());
    }

    private void fillBuffer() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .waitTimeSeconds(properties.getWaitTimeSeconds())
                .maxNumberOfMessages(properties.getMaxMessages())
                .visibilityTimeout(properties.getVisibilityTimeoutSeconds())
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .build();
        sqs.receiveMessage(request).messages().forEach(message -> {
            log.info("Received SQS processing message {} with receive attempt {}", message.messageId(),
                    message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));
            bufferedMessages.add(toEnvelope(message));
        });
    }

    private ProcessingMessageEnvelope toEnvelope(Message message) {
        final ProcessingMessage payload;
        try {
            payload = objectMapper.readValue(message.body(), ProcessingMessage.class);
            validate(payload);
        } catch (Exception exception) {
            log.warn("Malformed processing message from SQS; message will remain available for redelivery: {}", message.messageId(), exception);
            throw new IllegalArgumentException("Malformed processing message", exception);
        }

        AtomicBoolean acknowledged = new AtomicBoolean();
        return new ProcessingMessageEnvelope(payload, () -> {
            if (acknowledged.compareAndSet(false, true)) {
                log.info("Acknowledging processing message {}", payload.messageId());
                sqs.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(properties.getQueueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        });
    }

    private void validate(ProcessingMessage message) {
        if (message == null || message.messageId() == null || message.messageId().isBlank()) throw new IllegalArgumentException("messageId is required");
        if (message.bucket() == null || message.bucket().isBlank()) throw new IllegalArgumentException("bucket is required");
        if (message.objectKey() == null || message.objectKey().isBlank()) throw new IllegalArgumentException("objectKey is required");
    }

    @Override
    @PreDestroy
    public void close() {
        sqs.close();
    }
}
