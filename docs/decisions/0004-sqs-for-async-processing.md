# ADR-0004: Use SQS for asynchronous document processing

- Status: Accepted
- Date: 2026-09-29

## Context

Processing must not extend the upload request and may have variable duration or failure behavior. The platform needs retry, backpressure, and dead-letter handling.

## Decision

Persist a processing job in PostgreSQL and dispatch work through SQS. Consumers must be idempotent, use bounded retries, and publish exhausted failures to a dead-letter queue. Use Lambda only for small event-driven glue where it reduces operational work.

## Consequences

Uploads remain responsive and worker capacity can scale independently. SQS provides at-least-once delivery, so duplicate delivery and job claiming are part of the design. Reliable publication requires an outbox or reconciliation mechanism.
