# ADR-0001: Start with a modular Spring Boot monolith

- Status: Accepted
- Date: 2026-09-29

## Context

CloudDocs is new and has no demonstrated independent scaling, team ownership, or deployment bottleneck. Microservices would add network, deployment, observability, and data-consistency costs before those costs are justified.

## Decision

Build the first release as one Spring Boot deployment with explicit business modules, application ports, domain rules, and module contracts. Keep the deployment monolithic while preserving extraction seams.

## Consequences

The project has one straightforward local and AWS deployment and simple transactions. Module discipline is required to prevent a distributed monolith inside one process. Extraction will require contract hardening later, but that cost is deferred until evidence supports it.
