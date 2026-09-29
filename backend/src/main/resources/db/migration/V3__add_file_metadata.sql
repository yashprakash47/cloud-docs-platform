ALTER TABLE document_versions RENAME COLUMN media_type TO content_type;

ALTER TABLE document_versions
    ADD COLUMN original_filename VARCHAR(255);

CREATE INDEX document_versions_tenant_content_type_idx
    ON document_versions (tenant_id, content_type);
