# CloudDocs Architecture

## 1. Architectural intent

CloudDocs starts as a modular monolith: one Spring Boot deployment, one PostgreSQL database, and a React/Vite client. Modules are separated by business responsibility and communicate through application interfaces and domain events, not by direct access to one another's persistence internals. This keeps local development and deployment simple while preserving extraction seams for later growth.

The first architecture optimizes for explainability, correctness, and observable workflows. Microservices are introduced only when an independently scalable, deployable, or owned boundary has a demonstrated need.

## 2. Initial system shape

```text
Browser (React + TypeScript + Vite)
                 |
        HTTPS / JSON API
                 |
      Spring Boot modular monolith
  identity | tenant | document | folder/tag
  processing | search | audit | operations
          |                         |
   PostgreSQL (RDS)             S3 document objects
          |
      outbox/job state
          |
       SQS queues  <---- Lambda/S3 event trigger where useful
          |
   in-process worker or separate worker profile
```

EC2 hosts the stateless application behind an AWS load balancer when production scale requires multiple instances. RDS PostgreSQL stores transactional metadata. S3 stores binaries. SQS decouples processing from request handling. Lambda is reserved for small event-driven triggers and integration glue, not as a second general-purpose application runtime.

## 3. Monolith modules

Each module owns its use cases, domain rules, API models, and persistence adapters. A module may expose a small application interface or event contract. Cross-module database joins are avoided in application code where a module boundary would otherwise be weakened.

### Identity and access

Maps authenticated principals to users, tenants, memberships, and roles. It supplies authorization decisions and identity context to other modules. Provider-specific token/session handling is isolated here.

### Tenant and membership

Owns tenant lifecycle, membership invitations/status, role assignment, and tenant-scoped configuration. It does not own document permissions beyond the role/policy facts consumed by authorization.

### Document catalog

Owns logical documents, versions, lifecycle status, metadata, checksums, storage references, and archive behavior. It coordinates with the storage adapter but does not embed S3 SDK calls in domain logic.

### Folder and tagging

Owns folder hierarchy, document-folder association, tags, and supported organization queries. It validates tenant-scoped relationships.

### Storage

Provides an application-facing object-storage port for create, read/download, verify, and delete/cleanup operations. Its AWS S3 adapter owns object-key conventions, metadata headers, and presigned URL policy if presigned transfers are later enabled.

### Processing

Owns processing jobs, attempts, state transitions, retry policy, idempotency, and processor registration. Initial processors perform file validation and metadata extraction. The module consumes durable job messages and emits completion/failure events.

### Search/query

Owns read-oriented query composition and pagination for the initial PostgreSQL-backed search. It can later become a search-index adapter or service without changing document write ownership.

### Audit

Owns append-oriented audit records and audit queries. Other modules publish audit facts through an application port/event rather than writing audit rows directly.

### Operations

Owns health/readiness checks, metrics conventions, correlation IDs, error mapping, and operational endpoints. It must not contain business rules.

## 4. Request and dependency rules

- Controllers translate HTTP requests to application commands/queries and return API DTOs.
- Domain objects do not depend on Spring, AWS SDKs, or web types.
- Application services orchestrate transactions and ports.
- Infrastructure adapters implement ports for PostgreSQL, S3, SQS, identity, and email/integration needs.
- Modules may depend on another module's published application contract, not its repositories or tables.
- Events that cross a reliability boundary are persisted durably before publication or are recoverable through an outbox/reconciliation process.

## 5. Data model

All tables include `id`, `created_at`, and where relevant `updated_at`; tenant-owned tables include `tenant_id` and indexes begin with that scope key.

```text
User 1---* Membership *---1 Tenant
Tenant 1---* Folder
Folder 1---* Folder              (parent-child)
Tenant 1---* Document
Document 1---* DocumentVersion
Document *---* Tag               (DocumentTag)
DocumentVersion 1---* ProcessingJob
ProcessingJob 1---* ProcessingAttempt
Tenant 1---* AuditEvent
User 1---* AuditEvent             (actor, nullable for system actions)
```

### Core entities

- **User:** stable identity, display attributes, account status; no document binary data.
- **Tenant:** organizational boundary and configuration.
- **Membership:** user-to-tenant relationship, role, invitation/status timestamps.
- **Folder:** tenant-scoped name and optional parent folder; enforce no cross-tenant parent.
- **Document:** stable logical ID, tenant, current version reference, folder/location, lifecycle status, owner, archive timestamp.
- **DocumentVersion:** immutable version number, S3 bucket/key, media type, size, checksum, uploader, upload completion state.
- **Tag:** tenant-scoped name and normalized value.
- **DocumentTag:** association between a document version-independent logical document and tag.
- **ProcessingJob:** document version, type, status, idempotency key, attempt count, next retry time, failure code/message, timestamps.
- **ProcessingAttempt:** attempt number, worker identity, start/end, outcome, bounded diagnostic reference.
- **AuditEvent:** tenant, actor or system subject, action, target type/id, outcome, correlation ID, timestamp, safe structured details.

Foreign keys and unique constraints enforce tenant consistency. Recommended indexes include `(tenant_id, updated_at)`, `(tenant_id, name)`, `(tenant_id, status)`, document version `(document_id, version_number)`, job `(status, next_attempt_at)`, and audit `(tenant_id, occurred_at)`.

## 6. AWS architecture

### Initial deployment

- **VPC:** public subnets for load balancer; private subnets for EC2 and RDS. S3/SQS access uses IAM roles and, where justified, VPC endpoints.
- **EC2:** deploy the containerized Spring Boot monolith. Use an Auto Scaling Group when multiple instances are needed; instances remain stateless.
- **Load balancing:** HTTPS termination and health-checked routing through an Application Load Balancer.
- **RDS PostgreSQL:** private subnet deployment, automated backups, encryption, parameterized sizing, and Multi-AZ as the availability target justifies it.
- **S3:** private bucket, block public access, encryption, versioning/lifecycle policy as required, and object keys that do not contain sensitive names.
- **SQS:** one processing queue initially, plus a dead-letter queue. Configure visibility timeout, retention, redrive policy, and idempotent consumers.
- **Lambda:** small S3/SQS-triggered glue such as validating an object-created event or forwarding a normalized event. It does not duplicate catalog ownership.
- **Secrets/configuration:** AWS Secrets Manager or Parameter Store for credentials and environment configuration; no secrets in images or repositories.
- **Observability:** CloudWatch logs/metrics/alarms initially; application emits correlation IDs and structured events.

CI/CD with GitHub Actions is a later roadmap phase. It should build/test the monolith image, scan dependencies/image, publish to a registry, and deploy with an explicit environment approval path.

## 7. Upload and processing flow

1. Client requests an upload intent with filename, media type, size, target folder, and optional tags.
2. Backend authenticates, authorizes tenant/folder access, validates limits, creates a document/version in `PENDING_UPLOAD`, and returns a constrained upload mechanism (initially application-streamed upload; presigned S3 upload may be added when justified).
3. Binary is written to a tenant-safe S3 key. The backend verifies completion, size, media type, and checksum where available.
4. A transaction marks the version upload complete and creates a processing job with an idempotency key.
5. A reliable publication mechanism places the job on SQS. An outbox table or reconciliation scan is used if direct post-commit publication can lose messages.
6. A worker receives the message, claims the job, validates that the version is still eligible, and executes the registered processor.
7. The worker records attempt/result state and updates document availability. Duplicate delivery is safe because job/version state transitions and idempotency checks are enforced in PostgreSQL.
8. Transient failures retry with bounded backoff. Exhausted or non-retryable failures go to a dead-letter path and become visible to the user and operators.
9. The UI polls or later subscribes to status changes; it never infers success solely from an upload HTTP response.

Failure considerations: an object with no committed metadata is cleaned up by a reconciliation job; committed metadata with a missing object is marked inconsistent and alerted; a job message may be redelivered; a worker crash leaves visibility timeout recovery; processing must never mutate immutable version identity.

## 8. Evolution to microservices

The monolith is the initial deployment unit, not the domain model. Before extraction, measure load, change cadence, ownership, failure isolation, and operational cost. Extract only a boundary that has a clear technical reason such as independent scaling of processing, isolation of a failure-prone workload, or independently managed integration/security requirements.

### Likely service boundaries

- **Document Catalog Service:** document/version metadata and lifecycle.
- **Processing Service:** job orchestration and processors; likely first extraction because queue-driven work scales differently from APIs.
- **Identity/Tenant Service:** identity and membership only if multiple products or independent security ownership justify it.
- **Search Service:** only when PostgreSQL queries no longer meet search needs or an index has independent scale/lifecycle.
- **Audit Service:** only if audit retention, compliance, or write volume requires separate ownership.

Folder/tag functionality should initially remain with the catalog unless it develops independent lifecycle or scale. Storage is a capability adapter, not automatically a service.

### Extraction sequence

1. Make the module contract explicit and remove cross-module table/repository access.
2. Add integration-contract tests and publish domain events through an outbox.
3. Move one consumer (preferably Processing) behind a queue and deploy it separately while the monolith remains the system of record during migration.
4. Introduce an API/event ownership boundary and dual-read or shadow validation only where needed.
5. Transfer write ownership, backfill/verify data, then remove the old write path.

## 9. Database migration boundaries

Initial PostgreSQL schemas/tables may be in one database for transaction simplicity, but ownership is explicit:

- `identity` / `tenant`: users, tenants, memberships, tenant settings.
- `catalog`: documents, versions, folders, tags, associations.
- `processing`: jobs, attempts, processor results.
- `audit`: audit events.

At first these may be PostgreSQL schemas or clearly named tables in one database. A migration to service-owned databases is justified when a service requires independent scaling/availability, independent retention/compliance, release autonomy, or failure isolation. Migration requires an inventory of foreign keys, replacement of cross-schema joins with APIs/events, data backfill and reconciliation, a cutover plan, and rollback/dual-write strategy. Do not split tables merely to imitate microservices.

## 10. Security and failure posture

Use least privilege for EC2/Lambda roles, private S3/RDS, strict tenant predicates, input/content validation, dependency updates, safe error responses, and secret rotation. Log correlation IDs and event outcomes but never document contents, tokens, or credentials. Define alert thresholds for upload inconsistency, failed jobs, DLQ depth, database health, latency, and disk/connection pressure.
