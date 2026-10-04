package com.clouddocs.processing.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clouddocs.processing.sqs")
public class SqsProcessingProperties {
    private String queueUrl;
    private String region = "ap-south-1";
    private int waitTimeSeconds = 20;
    private int maxMessages = 10;
    private int visibilityTimeoutSeconds = 120;

    public String getQueueUrl() { return queueUrl; }
    public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public int getWaitTimeSeconds() { return waitTimeSeconds; }
    public void setWaitTimeSeconds(int waitTimeSeconds) { this.waitTimeSeconds = waitTimeSeconds; }
    public int getMaxMessages() { return maxMessages; }
    public void setMaxMessages(int maxMessages) { this.maxMessages = maxMessages; }
    public int getVisibilityTimeoutSeconds() { return visibilityTimeoutSeconds; }
    public void setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) { this.visibilityTimeoutSeconds = visibilityTimeoutSeconds; }

    public void validate() {
        if (queueUrl == null || queueUrl.isBlank()) throw new IllegalStateException("CLOUDDOCS_PROCESSING_QUEUE_URL is required when SQS processing is enabled");
        if (region == null || region.isBlank()) throw new IllegalStateException("AWS_REGION is required when SQS processing is enabled");
        if (waitTimeSeconds < 0 || waitTimeSeconds > 20) throw new IllegalArgumentException("SQS wait time must be between 0 and 20 seconds");
        if (maxMessages < 1 || maxMessages > 10) throw new IllegalArgumentException("SQS max messages must be between 1 and 10");
        if (visibilityTimeoutSeconds < 0) throw new IllegalArgumentException("SQS visibility timeout cannot be negative");
    }
}
