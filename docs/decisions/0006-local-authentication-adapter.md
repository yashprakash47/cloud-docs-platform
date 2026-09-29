# ADR-0006: Use a replaceable local authentication adapter for Phase 2

- Status: Accepted
- Date: 2026-09-29

## Context

CloudDocs needs an authenticated user context to exercise tenant and membership authorization, but an external identity provider is outside Phase 2. The local mechanism must not introduce provider coupling or secrets.

## Decision

Expose an `AuthenticationService` application boundary and implement it locally by reading a validated `X-User-Email` header. In the local profile, the adapter may provision an active user record with a derived display name. Production authentication providers will implement the same boundary later; this header mechanism is not a production security control.

## Consequences

Local API and integration testing can exercise real tenant authorization without credentials or external services. The adapter must be disabled/replaced before any non-local deployment, and external identity claims must be mapped to the stable `User` entity rather than leaking provider details into tenancy or document modules.
