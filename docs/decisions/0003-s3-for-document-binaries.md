# ADR-0003: Store document binaries in private S3

- Status: Accepted
- Date: 2026-09-29

## Context

Document binaries can be large and have different durability, lifecycle, and access characteristics from relational metadata.

## Decision

Store binaries in a private, encrypted S3 bucket. PostgreSQL stores immutable version metadata and the S3 object reference. Application authorization controls access; storage credentials are never exposed to users.

## Consequences

S3 provides durable, scalable binary storage and lifecycle controls. Uploads are multi-step and can become inconsistent, so completion state, checksums, cleanup, and reconciliation are required.
