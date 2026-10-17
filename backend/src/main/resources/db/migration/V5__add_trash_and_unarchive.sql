ALTER TABLE documents ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_chk;
ALTER TABLE documents ADD CONSTRAINT documents_status_chk CHECK (status IN ('ACTIVE', 'ARCHIVED', 'TRASH'));

CREATE INDEX IF NOT EXISTS documents_deleted_at_idx ON documents (deleted_at);
