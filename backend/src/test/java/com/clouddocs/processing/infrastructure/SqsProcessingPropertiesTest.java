package com.clouddocs.processing.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqsProcessingPropertiesTest {
    @Test
    void acceptsConfiguredSqsPollingValues() {
        SqsProcessingProperties properties = new SqsProcessingProperties();
        properties.setQueueUrl("queue-url");
        properties.setRegion("ap-south-1");
        properties.setWaitTimeSeconds(20);
        properties.setMaxMessages(10);
        properties.setVisibilityTimeoutSeconds(120);
        assertDoesNotThrow(properties::validate);
    }

    @Test
    void rejectsMissingQueueAndInvalidPollingValues() {
        SqsProcessingProperties properties = new SqsProcessingProperties();
        assertThrows(IllegalStateException.class, properties::validate);
        properties.setQueueUrl("queue-url");
        properties.setWaitTimeSeconds(21);
        assertThrows(IllegalArgumentException.class, properties::validate);
    }
}
