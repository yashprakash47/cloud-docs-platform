# CloudDocs Platform

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring_Boot-3.5.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![AWS](https://img.shields.io/badge/AWS-Cloud-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)
[![CI](https://img.shields.io/github/actions/workflow/status/yashprakash47/cloud-docs-platform/ci.yml?branch=main&label=CI&style=for-the-badge)](https://github.com/yashprakash47/cloud-docs-platform/actions)

> **Live Application**: [http://65.0.151.57:8080/](http://65.0.151.57:8080/)  
> **Actuator Health**: [http://65.0.151.57:8080/actuator/health](http://65.0.151.57:8080/actuator/health)

CloudDocs is a production-grade, multi-tenant document management and asynchronous processing platform. It integrates a modular Spring Boot 3.5.5 backend with an embedded React SPA, backed by Amazon Web Services (S3, SQS, Lambda, RDS PostgreSQL, EC2) and containerized workflows.

---

## Architecture Overview

```text
+---------------------------------------------------------------------------------------+
|                                     BROWSER CLIENT                                    |
|             (React 18 + TypeScript + Vite SPA served via Spring Boot on EC2)          |
+---------------------------------------------------------------------------------------+
        |                                        |                                  ^
        | 1. Upload Intent & Metadata            | 2. File Upload                   | 7. Stream Download
        v                                        v                                  |
+--------------------+                   +--------------------+                     |
| Spring Boot Backend|                   |  Amazon S3 Bucket  |---------------------+
| (EC2 on Port 8080) |                   | (Document Storage) |
+--------------------+                   +--------------------+
        |                                        |
        | Updates State                          | 3. ObjectCreated Event
        v                                        v
+--------------------+                   +--------------------+
| AWS RDS PostgreSQL |                   |   AWS Lambda Fn    |
| (Multi-tenant DB)  |                   | (Event Transformer)|
+--------------------+                   +--------------------+
        ^                                        |
        |                                        | 4. Dispatches SQS Message
        |                                        v
        |                                +--------------------+
        | 6. Updates Job Status          |   AWS SQS Queue    |
        |    (Metadata / Status)         | (Processing Queue) |
        |                                +--------------------+
        |                                        |
        +----------------------------------------+
              5. Long-polling Consumer Worker
                 (EC2 Spring Boot Worker)
```

---

## Key Features

- **Direct Upload Intent Lifecycle**: Multi-step file ingestion workflow that validates file constraints (`text/plain`, `application/pdf`, `image/png`), reserves an intent record, accepts binary content, and completes the upload.
- **Event-Driven Asynchronous Processing Pipeline**: S3 upload triggers an AWS Lambda function that normalizes the event and dispatches it into an AWS SQS queue. The EC2 backend worker polls SQS, executes processing jobs with retry mechanisms, and logs lifecycle audits.
- **Secure Document Storage & Downloads**: Content-addressed document storage supporting AWS S3 or container-local file storage. Direct streaming download endpoints preserve original filenames via sanitized `Content-Disposition` headers.
- **Hierarchical Folders & Document Organization**: Dynamic folder tree creation, folder assignment, and filtering to organize enterprise documents across nested paths.
- **Tag Management & Classification**: Tenant-scoped document tagging with real-time tag attachment, detachment, and color-coded chips.
- **Multi-Tenant Isolation**: Tenant and user boundary enforcement ensuring complete isolation across workspaces, memberships, documents, and processing jobs.
- **Unified Single-Host Production Deployment**: The compiled React SPA is embedded directly into Spring Boot static resources with client-side SPA route forwarding (`SpaWebConfig`), eliminating cross-origin CORS latency and complexity.

---

## Tech Stack Summary

| Layer | Technology | Version / Specification |
| --- | --- | --- |
| **Backend Monolith** | Java, Spring Boot, Spring MVC | Java 21 (Temurin), Spring Boot 3.5.5 |
| **Persistence & Migration** | Spring Data JPA, Hibernate, Flyway | PostgreSQL 18 / H2 in-memory, Flyway 4 migrations |
| **Frontend SPA** | React, TypeScript, Vite | React 18, Vite 8, Node.js 20 |
| **Cloud Storage** | Amazon S3 | AWS SDK for Java v2 (`s3`) |
| **Messaging & Serverless** | AWS Lambda, Amazon SQS | Event-driven processing with visibility timeouts & DLQ |
| **Infrastructure & Compute** | AWS EC2, Docker, AWS RDS | Amazon Linux 2023, Docker Engine 25, RDS PostgreSQL |
| **CI / Automation** | GitHub Actions | Automated parallel backend testing and frontend build verification |

---

## Repository Structure

```text
cloud-docs-platform/
├── .github/
│   └── workflows/
│       └── ci.yml             # GitHub Actions CI pipeline
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/clouddocs/
│   │   │   │   ├── document/   # Documents, Folders, Tags, Uploads, Downloads
│   │   │   │   ├── identity/   # User provisioning and auth contexts
│   │   │   │   ├── operations/ # CORS, SPA forwarding, Global exceptions
│   │   │   │   ├── processing/ # SQS consumer, worker, processing jobs
│   │   │   │   ├── storage/    # S3 and local file storage adapters
│   │   │   │   └── tenancy/    # Multi-tenant isolation and memberships
│   │   │   └── resources/
│   │   │       ├── db/migration/ # Flyway SQL migrations
│   │   │       ├── static/       # Compiled React production SPA assets
│   │   │       └── application.yml
│   │   └── test/               # Unit and integration test suites
│   ├── pom.xml
│   └── mvnw
├── frontend/
│   ├── src/
│   │   ├── app/                # React components & UI views
│   │   └── services/           # Typed API client
│   ├── package.json
│   └── vite.config.ts
└── docker-compose.yml          # Local containerized PostgreSQL + backend
```

---

## Local Development

### 1. Prerequisites
- **Java 21** (Eclipse Temurin recommended)
- **Node.js 20+** and **npm**
- **Docker Engine 24+** (optional, for Docker Compose stack)

---

### 2. Run with Docker Compose (PostgreSQL Stack)

From the repository root:

```powershell
docker compose up --build
```

- Starts PostgreSQL and the Spring Boot backend.
- Application URL: `http://localhost:8080/`
- Actuator Health: `http://localhost:8080/actuator/health`

---

### 3. Run Locally (Stand-alone Process)

#### Start the Backend (H2 In-Memory)
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*The default profile runs on `http://localhost:8080` with in-memory H2 database and local file storage without requiring AWS credentials.*

#### Start Frontend Dev Server (Vite)
```powershell
cd frontend
npm install
npm run dev
```
*Vite launches on `http://localhost:5173` with proxying to `/api` and `/actuator`.*

---

### 4. Build and Validate

To run full automated verification locally:

```powershell
# Run backend tests (36+ tests)
cd backend
.\mvnw.cmd clean test

# Run frontend typecheck and production build
cd ..\frontend
npm ci
npm run typecheck
npm run build
```

---

## License

This project is licensed under the MIT License.
