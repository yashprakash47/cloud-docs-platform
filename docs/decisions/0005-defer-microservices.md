# ADR-0005: Require evidence before extracting microservices or databases

- Status: Accepted
- Date: 2026-09-29

## Context

The product has a planned evolution toward microservices and service-owned databases, but premature extraction would create operational and consistency complexity without known benefit.

## Decision

Evaluate extraction using measured independent scaling, failure isolation, release cadence, security/compliance ownership, team autonomy, data retention, and operational cost. The first candidate is likely Processing because queue-driven work has a different scaling and failure profile. Extract one boundary at a time with explicit contracts, reconciliation, and rollback.

## Consequences

The team avoids technology-driven fragmentation and can still evolve incrementally. A migration may take longer than a greenfield split, and measurements/runbooks are prerequisites rather than optional documentation.
