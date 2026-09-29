CREATE TABLE folders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    parent_id UUID NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT folders_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT folders_tenant_id_uk UNIQUE (tenant_id, id),
    CONSTRAINT folders_parent_tenant_fk FOREIGN KEY (tenant_id, parent_id)
        REFERENCES folders (tenant_id, id)
);

CREATE UNIQUE INDEX folders_tenant_parent_name_uk
    ON folders (tenant_id, parent_id, name);
CREATE INDEX folders_tenant_parent_idx ON folders (tenant_id, parent_id);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    folder_id UUID NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    current_version_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT documents_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT documents_owner_fk FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT documents_tenant_id_uk UNIQUE (tenant_id, id),
    CONSTRAINT documents_folder_tenant_fk FOREIGN KEY (tenant_id, folder_id)
        REFERENCES folders (tenant_id, id),
    CONSTRAINT documents_status_chk CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX documents_tenant_updated_idx ON documents (tenant_id, updated_at DESC);
CREATE INDEX documents_tenant_status_idx ON documents (tenant_id, status);
CREATE INDEX documents_tenant_folder_idx ON documents (tenant_id, folder_id);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    storage_reference VARCHAR(500) NOT NULL,
    media_type VARCHAR(255),
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(128),
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT document_versions_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT document_versions_document_tenant_fk FOREIGN KEY (tenant_id, document_id)
        REFERENCES documents (tenant_id, id),
    CONSTRAINT document_versions_uploader_fk FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT document_versions_tenant_id_uk UNIQUE (tenant_id, id),
    CONSTRAINT document_versions_number_uk UNIQUE (tenant_id, document_id, version_number),
    CONSTRAINT document_versions_number_chk CHECK (version_number > 0),
    CONSTRAINT document_versions_size_chk CHECK (size_bytes >= 0)
);

CREATE INDEX document_versions_document_idx
    ON document_versions (tenant_id, document_id, version_number DESC);

ALTER TABLE documents
    ADD CONSTRAINT documents_current_version_tenant_fk FOREIGN KEY (tenant_id, current_version_id)
        REFERENCES document_versions (tenant_id, id);

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tags_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT tags_tenant_id_uk UNIQUE (tenant_id, id),
    CONSTRAINT tags_tenant_name_uk UNIQUE (tenant_id, name)
);

CREATE INDEX tags_tenant_name_idx ON tags (tenant_id, name);

CREATE TABLE document_tags (
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, document_id, tag_id),
    CONSTRAINT document_tags_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT document_tags_document_tenant_fk FOREIGN KEY (tenant_id, document_id)
        REFERENCES documents (tenant_id, id),
    CONSTRAINT document_tags_tag_tenant_fk FOREIGN KEY (tenant_id, tag_id)
        REFERENCES tags (tenant_id, id)
);

CREATE INDEX document_tags_tag_idx ON document_tags (tenant_id, tag_id);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT audit_events_tenant_fk FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT audit_events_actor_fk FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX audit_events_tenant_occurred_idx ON audit_events (tenant_id, occurred_at DESC);
