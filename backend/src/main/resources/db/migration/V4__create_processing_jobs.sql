CREATE TABLE processing_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    completed_at TIMESTAMP WITH TIME ZONE NULL,
    failure_reason VARCHAR(2000) NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    message_id VARCHAR(255) NOT NULL,
    CONSTRAINT processing_jobs_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT processing_jobs_document_fk FOREIGN KEY (tenant_id, document_id)
        REFERENCES documents (tenant_id, id),
    CONSTRAINT processing_jobs_version_fk FOREIGN KEY (tenant_id, document_version_id)
        REFERENCES document_versions (tenant_id, id),
    CONSTRAINT processing_jobs_tenant_id_uk UNIQUE (tenant_id, id),
    CONSTRAINT processing_jobs_status_chk CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT processing_jobs_attempt_count_chk CHECK (attempt_count >= 0),
    CONSTRAINT processing_jobs_message_id_uk UNIQUE (message_id)
);

CREATE INDEX processing_jobs_tenant_created_idx
    ON processing_jobs (tenant_id, created_at DESC);
CREATE INDEX processing_jobs_document_idx
    ON processing_jobs (tenant_id, document_id, created_at DESC);
CREATE INDEX processing_jobs_status_idx
    ON processing_jobs (tenant_id, status, created_at DESC);

CREATE TABLE processing_attempts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    processing_job_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(2000) NULL,
    CONSTRAINT processing_attempts_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT processing_attempts_job_fk FOREIGN KEY (tenant_id, processing_job_id)
        REFERENCES processing_jobs (tenant_id, id),
    CONSTRAINT processing_attempts_number_chk CHECK (attempt_number > 0),
    CONSTRAINT processing_attempts_status_chk CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT processing_attempts_number_uk UNIQUE (processing_job_id, attempt_number)
);

CREATE INDEX processing_attempts_job_idx
    ON processing_attempts (tenant_id, processing_job_id, attempt_number);
