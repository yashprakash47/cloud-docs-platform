# S3 to Processing Queue Lambda

This standalone Java 21 AWS Lambda receives S3 `ObjectCreated` notifications and publishes one explicit processing message per S3 record to the existing `clouddocs-processing` SQS queue.

It is independent from the Spring Boot backend. It does not configure S3 notifications, SQS, IAM, or any other AWS resource.

## Configuration

Set these Lambda environment variables:

- `CLOUDDOCS_PROCESSING_QUEUE_URL` — required SQS queue URL.
- `AWS_REGION` — AWS region, currently `ap-south-1`.

The AWS SDK default credential/provider chain is used. Local commands can select the configured CLI profile with `AWS_PROFILE=cloud-docs`. No credentials belong in this module or repository.

## Handler

```text
com.clouddocs.lambda.S3ToProcessingQueueHandler
```

The handler publishes one JSON message for each S3 event record. The message contains `messageId`, `eventType`, `bucket`, `objectKey`, `s3EventName`, optional `versionId`, optional `eTag`, optional `size`, optional `eventTime`, and `source`.

S3 notifications are at-least-once. The message ID is a deterministic SHA-256 identifier derived from stable S3 event fields where available. Final deduplication and idempotency remain responsibilities of the future Processing Service.

## Local validation

Normal tests use mocked SQS and do not need AWS credentials:

```powershell
mvn test
mvn package
```

The deployable shaded Lambda artifact is produced at:

```text
target/s3-to-processing-queue-0.1.0-SNAPSHOT.jar
```

## Required future permissions

The eventual Lambda execution role should have only:

- `sqs:SendMessage` on the CloudDocs processing queue
- CloudWatch Logs permissions required for Lambda logging

Those policies are intentionally not created here.
