# CloudDocs Roadmap

The phases are intentionally small. Each phase leaves the repository in a demonstrably useful state and introduces only infrastructure that the phase needs.

## Phase 0 — Architecture baseline

**Objective:** Establish shared product scope, module boundaries, and decision records.

**Tasks:** Review requirements; agree on first-release constraints; create ADRs; define API/data naming conventions; record open questions.

**Dependencies:** Product owner input on target users, file limits, retention, and authentication direction.

**Acceptance criteria:** Requirements, architecture, roadmap, and ADRs are reviewed; out-of-scope items are explicit; no module claims ownership of another module's data.

**Validation/tests:** Architecture review checklist; trace each initial workflow to a module and data entity; verify every major choice has a rationale.

## Phase 1 — Repository and local skeleton

**Objective:** Create a buildable modular monolith and minimal React client shell.

**Tasks:** Establish Maven Java 21 backend; establish Vite TypeScript frontend; define package/module conventions; add local configuration examples; add formatting/static analysis; add health endpoint and a basic UI route.

**Dependencies:** Phase 0; Java/Node versions selected; local PostgreSQL strategy agreed.

**Acceptance criteria:** Backend and frontend build locally; health endpoint responds; no business workflow is implied to be complete; configuration contains no secrets.

**Validation/tests:** Maven test/package; frontend typecheck/build; clean checkout build; basic health smoke test.

## Phase 2 — Identity, tenancy, and authorization foundation

**Objective:** Establish secure tenant context and role checks before document features.

**Tasks:** Implement user/tenant/membership model; isolate authentication adapter; add role authorization; create migration tooling; add tenant-scoped repository/query conventions.

**Dependencies:** Phase 1; chosen authentication approach or a replaceable local stub for development.

**Acceptance criteria:** Authenticated user can select an authorized tenant; roles are enforced server-side; cross-tenant access tests fail safely.

**Validation/tests:** Unit authorization tests; integration tests for tenant isolation, membership status, and role changes; negative API tests.

## Phase 3 — Document catalog and metadata

**Objective:** Manage documents, versions, folders, tags, and metadata without binary storage yet.

**Tasks:** Add catalog schema/migrations; document/version lifecycle; folders/tags; paginated metadata queries; API error model; audit hooks for catalog changes.

**Dependencies:** Phase 2.

**Acceptance criteria:** Authorized users can create/list/update/archive metadata, create versions, organize documents, and see only their tenant's data.

**Validation/tests:** Domain unit tests; PostgreSQL integration tests; pagination/index checks; API contract tests; authorization matrix.

## Phase 4 — S3 storage and upload/download

**Objective:** Store document binaries durably and connect them to immutable versions.

**Tasks:** Define storage port and S3 adapter; private bucket policy; upload intent/completion flow; checksum/size validation; authorized download; orphan reconciliation path; local fake storage adapter.

**Dependencies:** Phase 3; AWS account/network and bucket policy available for integration testing.

**Acceptance criteria:** A valid authorized file can be uploaded, linked to a version, downloaded, and rejected safely when invalid; failed multi-step uploads are observable.

**Validation/tests:** Storage adapter tests; local end-to-end test with fake storage; AWS integration test in a non-production account; checksum and tenant-isolation tests; failure injection for orphan/missing-object cases.

## Phase 5 — Asynchronous processing

**Objective:** Add a reliable processing job lifecycle without blocking uploads.

**Tasks:** Add job/attempt entities; implement initial validation/metadata processor; add SQS adapter and DLQ; implement idempotency, retry/backoff, claim/lease, and reconciliation; expose status/retry API.

**Dependencies:** Phase 4; queue and IAM policy; operational choice of in-process worker profile versus separate worker deployment.

**Acceptance criteria:** Upload creates a job; worker completes or fails it visibly; duplicate delivery does not duplicate effects; exhausted failures reach a dead-letter path.

**Validation/tests:** State-machine unit tests; integration tests with queue emulator or test queue; duplicate/retry/crash simulations; end-to-end upload-to-status smoke test.

## Phase 6 — React workflow and usability

**Objective:** Deliver the first usable document-management experience.

**Tasks:** Implement sign-in/session shell; tenant/folder navigation; upload progress; document list/filter; detail/version view; processing status and retry; accessible error/loading/empty states.

**Dependencies:** Phases 2–5; stable API contracts.

**Acceptance criteria:** A test user can complete the main workflows from the browser; unauthorized controls are not presented as available; failures have actionable feedback.

**Validation/tests:** Component and route tests; browser smoke tests; keyboard/accessibility checks; API contract compatibility test.

## Phase 7 — Containerized AWS deployment

**Objective:** Run the monolith in a repeatable AWS environment.

**Tasks:** Create Docker image; configure VPC/private subnets; deploy EC2/ALB; provision RDS PostgreSQL and S3/SQS; configure IAM/secrets; establish database migrations and environment separation; add basic backups.

**Dependencies:** Phases 1–6; AWS account/network ownership; approved cost limits.

**Acceptance criteria:** Staging environment serves the app over HTTPS; data persists through instance replacement; RDS/S3/SQS access uses roles/secrets; health checks and rollback procedure are documented.

**Validation/tests:** Image vulnerability/dependency scan; staging smoke test; instance replacement test; database backup/restore drill; S3 access-policy test; queue redelivery/DLQ test.

## Phase 8 — CI/CD and release controls

**Objective:** Automate quality checks and controlled deployment.

**Tasks:** GitHub Actions for backend/frontend tests, build, scans, image publish, migrations, and staging deploy; environment approvals; artifact/version strategy; rollback runbook.

**Dependencies:** Phase 7; GitHub/AWS credentials via OIDC or an approved equivalent.

**Acceptance criteria:** Pull requests receive repeatable checks; a tagged release produces traceable artifacts; staging deploy is automated; production requires approval and has a rollback path.

**Validation/tests:** Run pipeline on clean branch; deliberately fail tests/scan; deploy two versions and execute rollback; verify secrets are not printed.

## Phase 9 — Observability, security, and resilience hardening

**Objective:** Make production behavior diagnosable and failure-aware.

**Tasks:** Add structured logs/correlation; metrics and dashboards; alarms; audit review; rate limits; dependency/security scanning; backup/restore and incident runbooks; load and fault tests.

**Dependencies:** Phase 7; representative staging traffic and operational ownership.

**Acceptance criteria:** Operators can trace an upload/job; key failure conditions alert; restore objectives are measured; security findings have an owner and remediation process.

**Validation/tests:** Load test; queue backlog test; database failover/restore drill; expired credential test; log redaction test; alert delivery test.

## Phase 10 — Evidence-based extraction of Processing service

**Objective:** Extract only the first boundary proven to need independent scaling or failure isolation.

**Tasks:** Measure processing load/failure profile; harden module contract/outbox; deploy processing consumer separately; transfer ownership of jobs; remove monolith worker path after reconciliation; document operational cost.

**Dependencies:** Phases 5, 7–9; stable event contract; migration/rollback plan; observed need.

**Acceptance criteria:** Processing can deploy/scale independently; duplicate and failure semantics remain correct; catalog remains available when processing is degraded; data ownership is unambiguous.

**Validation/tests:** Contract tests; shadow/dual-run comparison where safe; backpressure/failure tests; cutover and rollback rehearsal; data reconciliation report.

## Phase 11 — Further service-owned database evaluation

**Objective:** Decide whether additional service/database extraction is justified, rather than assuming it.

**Tasks:** Measure module coupling, query boundaries, retention/security requirements, and operational cost; replace cross-schema joins with APIs/events where a split is approved; backfill and verify one boundary at a time.

**Dependencies:** Phase 10 evidence and service ownership model.

**Acceptance criteria:** Each approved split has a quantified reason, explicit owner, migration/rollback plan, independent backups, and contract tests; unneeded splits are rejected and documented.

**Validation/tests:** Data consistency/reconciliation; migration rehearsal; failure isolation; backup/restore per database; latency and cost comparison.
