# ADR-0002: Use PostgreSQL as the authoritative metadata store

- Status: Accepted
- Date: 2026-09-29

## Context

CloudDocs needs transactional relationships among tenants, permissions, documents, versions, folders, tags, jobs, and audit records. The initial search requirements are metadata-oriented.

## Decision

Use PostgreSQL on RDS for transactional metadata and workflow state. Establish logical ownership schemas/modules from the beginning, but keep one database until independent ownership or scale is justified.

## Consequences

Foreign keys, transactions, and familiar operational tooling simplify correctness. S3 remains the binary authority, so cross-system reconciliation is required. Search complexity must be measured before introducing a separate search system.
