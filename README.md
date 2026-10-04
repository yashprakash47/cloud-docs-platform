# CloudDocs

CloudDocs is a document management and document processing platform. The repository contains a modular Spring Boot monolith, a React/Vite frontend, and an opt-in AWS event-processing path.

## Repository layout

```text
backend/     Java 21 + Spring Boot + Maven backend
frontend/    React + TypeScript + Vite frontend
docs/        Architecture decisions and project documentation
```

## Prerequisites

- Java 21
- Maven 3.9+ (the backend includes a Maven Wrapper entry point)
- Node.js 20+
- Docker Engine 24+ with Docker Compose v2 (for the containerized local stack)

The default process-based development mode uses an embedded H2 database and requires no AWS credentials. The Docker Compose mode uses PostgreSQL and runs the same Flyway migrations at startup.

### Run with Docker Compose

From the repository root:

```powershell
docker compose up --build
```

The stack starts PostgreSQL and the backend. PostgreSQL data is stored in the named `clouddocs-postgres-data` volume. The backend is available at `http://localhost:8080` and its health endpoint is `http://localhost:8080/actuator/health`.

Useful commands:

```powershell
docker compose logs -f backend
docker compose ps
docker compose down
```

`docker compose down` stops and removes containers but keeps the named database volume. To remove local database data as well, use `docker compose down -v`.

Compose defaults are intentionally local-only:

| Variable | Default | Purpose |
| --- | --- | --- |
| `CLOUDDOCS_DB_NAME` | `clouddocs` | PostgreSQL database name |
| `CLOUDDOCS_DB_USERNAME` | `clouddocs` | Local database user |
| `CLOUDDOCS_DB_PASSWORD` | `clouddocs-local-password` | Local-only database password |
| `CLOUDDOCS_POSTGRES_PORT` | `5432` | Host PostgreSQL port |
| `CLOUDDOCS_BACKEND_PORT` | `8080` | Host backend port |
| `CLOUDDOCS_STORAGE_PROVIDER` | `local` | `local` or `s3` |
| `CLOUDDOCS_PROCESSING_MESSAGE_SOURCE` | `local` | `local` or `sqs` |
| `CLOUDDOCS_LOCAL_STORAGE_DIRECTORY` | `/tmp/clouddocs-files` | Container-local file storage |
| `CLOUDDOCS_S3_BUCKET` | empty | Required only for S3 storage |
| `CLOUDDOCS_PROCESSING_QUEUE_URL` | empty | Required only for SQS processing |
| `AWS_REGION` | `ap-south-1` | AWS region when AWS adapters are enabled |

For a fully local container stack, keep `CLOUDDOCS_STORAGE_PROVIDER=local` and `CLOUDDOCS_PROCESSING_MESSAGE_SOURCE=local`. No AWS credentials are needed. S3 and SQS modes remain configurable through environment variables, but require an appropriate credential provider and network access; credentials must never be baked into the image or committed to the repository.

## Run the backend

From `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`. Health endpoints:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/health/readiness`

The local development authentication adapter accepts an `X-User-Email` request header and auto-provisions that user when running with the default local profile. This is intentionally not a production authentication mechanism.

To use PostgreSQL locally, copy `backend/src/main/resources/application-local.yml.example` to `application-local.yml` and provide `CLOUDDOCS_DB_PASSWORD` through the environment. Flyway applies migrations automatically at startup.

### Document storage

Local filesystem storage is the default and requires no AWS credentials:

```powershell
$env:CLOUDDOCS_STORAGE_PROVIDER = "local"
$env:CLOUDDOCS_LOCAL_STORAGE_DIRECTORY = "C:\\temp\\clouddocs-files"
.\mvnw.cmd spring-boot:run
```

To use the existing private S3 bucket, select the S3 provider and provide only configuration, never credentials:

```powershell
$env:CLOUDDOCS_STORAGE_PROVIDER = "s3"
$env:CLOUDDOCS_S3_BUCKET = "clouddocs-documents-315527"
$env:AWS_REGION = "ap-south-1"
$env:AWS_PROFILE = "cloud-docs"
.\mvnw.cmd spring-boot:run
```

The AWS SDK uses its default credential/provider chain. `AWS_PROFILE=cloud-docs` makes the local AWS CLI profile available to that chain. The application does not create public URLs and the bucket must remain private. Do not commit access keys, secrets, session tokens, `~/.aws` files, or local environment files.

S3 integration tests are disabled during normal test runs. To run the isolated upload/download/delete smoke test against the configured bucket:

```powershell
$env:CLOUDDOCS_S3_INTEGRATION_TEST = "true"
$env:CLOUDDOCS_STORAGE_PROVIDER = "s3"
$env:CLOUDDOCS_S3_BUCKET = "clouddocs-documents-315527"
$env:AWS_REGION = "ap-south-1"
$env:AWS_PROFILE = "cloud-docs"
cd backend
mvn test -Dtest=S3DocumentStorageIntegrationTest
```

### SQS processing source

Local development uses the in-memory processing message source by default. To enable the AWS SQS poller, provide the queue URL and region:

```powershell
$env:CLOUDDOCS_PROCESSING_MESSAGE_SOURCE = "sqs"
$env:CLOUDDOCS_PROCESSING_QUEUE_URL = "https://sqs.ap-south-1.amazonaws.com/<account-id>/clouddocs-processing"
$env:AWS_REGION = "ap-south-1"
$env:AWS_PROFILE = "cloud-docs"
$env:CLOUDDOCS_SQS_WAIT_TIME_SECONDS = "20"
$env:CLOUDDOCS_SQS_MAX_MESSAGES = "10"
$env:CLOUDDOCS_SQS_VISIBILITY_TIMEOUT_SECONDS = "120"
cd backend
mvnw.cmd spring-boot:run
```

The AWS SDK uses its default credential/provider chain; `AWS_PROFILE` is optional when another chain is configured. The poller deletes a message only after processing returns `SUCCEEDED`. Failed or malformed messages remain unacknowledged for SQS redelivery and DLQ handling. Never commit credentials or local AWS configuration.

The SQS adapter smoke test is disabled during normal test runs. To enable it against the existing queue, set `CLOUDDOCS_SQS_INTEGRATION_TEST=true` and run `mvn test -Dtest=SqsProcessingMessageSourceIntegrationTest` from `backend/`.

Phase 2 APIs:

- `GET /api/v1/me`
- `GET,POST /api/v1/tenants`
- `GET,POST /api/v1/tenants/{tenantId}/memberships`
- `PATCH /api/v1/tenants/{tenantId}/memberships/{membershipId}/role`
- `DELETE /api/v1/tenants/{tenantId}/memberships/{membershipId}`

## Run the frontend

From `frontend/`:

```powershell
npm install
npm run dev
```

The Vite development server starts on `http://localhost:5173`. Set `VITE_API_URL` when the backend runs at a different URL.

## Validate

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package

cd ..\frontend
npm run typecheck
npm run build
```

The containerization setup is intentionally limited to a local Spring Boot + PostgreSQL stack. AWS deployment resources, microservices, Kubernetes, and production secrets are not included.
