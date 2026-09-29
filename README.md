# CloudDocs

CloudDocs is a document management and document processing platform. This repository currently contains the Phase 1 local skeleton: a modular Spring Boot backend and a React/Vite frontend.

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

Phase 2 adds PostgreSQL support and Flyway migrations. By default, local startup uses an embedded H2 database in PostgreSQL compatibility mode so the skeleton can run without external services. PostgreSQL can be selected through the example configuration.

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

Phase 2 intentionally contains no document workflows, AWS integration, Docker configuration, or microservices.
