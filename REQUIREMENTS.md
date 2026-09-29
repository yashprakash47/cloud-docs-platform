# CloudDocs Requirements

## 1. Product definition

CloudDocs is a document management and document processing platform for authenticated users and organizations. It provides a controlled place to upload, organize, retrieve, inspect, and process documents while preserving document metadata, processing status, audit history, and access boundaries.

The first release is intentionally a modular Spring Boot monolith with a React client. It supports a small but meaningful document lifecycle and establishes contracts that can later be extracted into independently deployable services.

## 2. Goals and scope

### Goals

- Store documents durably and make them discoverable through metadata.
- Keep document binaries out of PostgreSQL and store them in Amazon S3.
- Track document lifecycle and processing state explicitly.
- Provide asynchronous processing for work that should not block an upload request.
- Enforce tenant/user authorization and produce an audit trail.
- Establish a simple, testable architecture that can evolve without premature microservices.

### Initial scope

- User accounts, organizations/tenants, and role-based access.
- Document metadata, folders, tags, versions, and lifecycle status.
- Direct document upload and download through application-authorized flows.
- A processing-job model with an initial metadata/validation processing capability.
- Search and filtering over document metadata.
- Audit events for security-relevant document actions.
- Operational health and structured application logging.

### Explicitly out of scope for the first release

- Full-text/OCR search, collaborative editing, public sharing links, e-signatures, billing, legal retention policies, and broad third-party integrations.
- Microservice deployment, service-owned databases, Kubernetes, event streaming platforms, and a service mesh.
- Multi-region active-active deployment.

## 3. Main user workflows

1. **Sign in and access a workspace**
   - A user signs in, is associated with a tenant, and sees only authorized folders and documents.

2. **Create an organizational structure**
   - An authorized user creates folders and optionally applies tags to support navigation and filtering.

3. **Upload a document**
   - The user selects a file and metadata. The platform validates size/type and authorization, stores the binary in S3, persists metadata/version state in PostgreSQL, and reports the document as processing or available.

4. **Monitor processing**
   - The user views processing status and any failure reason. Retry is available for retryable failures to authorized users.

5. **Find and inspect a document**
   - The user searches/filter documents by name, folder, tag, owner, type, status, and dates; opens metadata and version history; and downloads an authorized version.

6. **Update a document**
   - The user uploads a new version or changes permitted metadata. The prior version remains auditable and retrievable according to retention rules.

7. **Administer access**
   - A tenant administrator invites/deactivates users and assigns roles within the tenant.

8. **Review activity**
   - An authorized administrator reviews audit events for uploads, downloads, metadata changes, access changes, processing, and failures.

## 4. Functional requirements

### Identity, tenancy, and authorization

- FR-01: Users shall authenticate before accessing tenant data.
- FR-02: A user shall belong to one or more tenants with a selected active tenant context.
- FR-03: The system shall support at least `TENANT_ADMIN`, `MEMBER`, and `VIEWER` roles.
- FR-04: Every document, folder, tag, job, and audit event shall be tenant-scoped.
- FR-05: Authorization shall be enforced server-side for every document operation; UI checks are not security controls.

### Documents and metadata

- FR-06: Authorized users shall create, view, update, list, and archive documents.
- FR-07: A document shall have a stable logical identity independent of its versions.
- FR-08: Each version shall record an immutable S3 object key, size, media type, checksum when available, uploader, and creation time.
- FR-09: The system shall validate configured maximum size and an allowlist of supported media types before accepting a document.
- FR-10: Users shall organize documents in folders and apply/remove tags subject to authorization.
- FR-11: The system shall retain version history and identify the current version.
- FR-12: Downloads shall return only authorized content and shall not expose raw storage credentials.

### Processing

- FR-13: Upload completion shall create a processing job when processing is applicable.
- FR-14: Jobs shall expose `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, and `CANCELLED` states with timestamps and an actionable failure reason.
- FR-15: Processing shall be asynchronous and idempotent for the same document version/job attempt.
- FR-16: Authorized users shall view job status and retry retryable failures.

### Search and audit

- FR-17: Users shall filter and paginate documents by supported metadata fields.
- FR-18: The system shall record security-relevant actions with actor, tenant, target, action, outcome, and timestamp.
- FR-19: Tenant administrators shall be able to query audit events within their tenant.

### Administration and operations

- FR-20: Tenant administrators shall manage membership and role assignments.
- FR-21: The system shall expose liveness/readiness health information without exposing secrets.
- FR-22: Administrative configuration shall support document limits, allowed media types, and processing retry policy.

## 5. Non-functional requirements

- NFR-01 **Security:** TLS in transit; encryption at rest through AWS-managed encryption; least-privilege IAM; secrets kept outside source control; server-side tenant isolation; safe filename/content handling; auditability of authorization-sensitive actions.
- NFR-02 **Reliability:** Upload metadata and object state shall be reconciled when a multi-step upload fails. Processing shall tolerate duplicate delivery and support bounded retries.
- NFR-03 **Performance:** Paginated metadata queries should normally complete within 500 ms at the application boundary under the initial target load. Upload response time should not wait for asynchronous processing.
- NFR-04 **Scalability:** Stateless application instances; S3 for binary scale; SQS-backed worker concurrency; database indexes for tenant and common search predicates.
- NFR-05 **Availability:** Initial production target is a single AWS region with an RDS Multi-AZ option and replaceable EC2 instances; exact SLA is deferred until capacity and support requirements are known.
- NFR-06 **Consistency:** PostgreSQL is authoritative for metadata and job state. S3 is authoritative for binary content. Cross-system transitions must be observable and reconciled.
- NFR-07 **Maintainability:** Modular boundaries, explicit use cases, constructor dependency injection, migrations, API contracts, automated tests, and ADR-backed architectural changes.
- NFR-08 **Observability:** Correlated structured logs, metrics for requests/uploads/jobs/queue depth/errors, and actionable alarms for failures and backlog.
- NFR-09 **Privacy:** Minimize stored personal data, define retention/deletion behavior, and avoid putting document contents or credentials in logs.
- NFR-10 **Recoverability:** Automated database backups, S3 versioning/lifecycle policy where appropriate, documented restore procedures, and tested recovery objectives before production claims.

## 6. Initial quality targets

- Unit tests for domain rules and application use cases.
- Integration tests for PostgreSQL, S3 interaction boundaries, authorization, and job state transitions.
- API-level tests for happy paths, invalid input, tenant isolation, and retry behavior.
- Frontend tests for routing, upload state, permissions-aware rendering, and error states.
- A smoke test covering sign-in/session, upload, processing completion, search, and download in a deployed environment.

## 7. Assumptions and open decisions

- The first release is a single-tenant-context-at-a-time user experience, even though accounts may belong to multiple tenants.
- Authentication provider choice is intentionally deferred; the backend must isolate identity-provider details behind an application boundary.
- Initial processing means validation and metadata extraction; OCR/full-text is a later capability requiring explicit capacity and privacy analysis.
- Exact file limits, retention policy, SLA, compliance requirements, and target scale require product confirmation before production hardening.
