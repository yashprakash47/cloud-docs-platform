package com.clouddocs.document.infrastructure;

import com.clouddocs.document.domain.*;
import com.clouddocs.operations.api.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcDocumentRepository {
    private final JdbcTemplate jdbc;
    public JdbcDocumentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Document insert(UUID tenantId, UUID ownerId, UUID folderId, String name, String description) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO documents(id,tenant_id,owner_user_id,folder_id,name,description,status) VALUES (?,?,?,?,?,?,'ACTIVE')",
                id, tenantId, ownerId, folderId, name, description);
        return findByIdAndTenant(id, tenantId);
    }
    public Document findByIdAndTenant(UUID id, UUID tenantId) {
        return jdbc.query("SELECT * FROM documents WHERE id=? AND tenant_id=?", this::map, id, tenantId)
                .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Document not found in this tenant"));
    }
    public PageResponse<Document> findPage(UUID tenantId, DocumentStatus status, int page, int size) {
        String filter = status == null ? "" : " AND status=?";
        Object[] args = status == null ? new Object[]{tenantId, size, page * size} : new Object[]{tenantId, status.name(), size, page * size};
        long total = status == null ? jdbc.queryForObject("SELECT COUNT(*) FROM documents WHERE tenant_id=?", Long.class, tenantId)
                : jdbc.queryForObject("SELECT COUNT(*) FROM documents WHERE tenant_id=? AND status=?", Long.class, tenantId, status.name());
        List<Document> items = jdbc.query("SELECT * FROM documents WHERE tenant_id=?" + filter + " ORDER BY created_at DESC LIMIT ? OFFSET ?", this::map, args);
        return new PageResponse<>(items, page, size, total, (int) Math.ceil((double) total / size));
    }
    public void update(UUID id, UUID tenantId, String name, String description, UUID folderId) {
        jdbc.update("UPDATE documents SET name=?, description=?, folder_id=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", name, description, folderId, id, tenantId);
    }
    public void archive(UUID id, UUID tenantId) {
        jdbc.update("UPDATE documents SET status='ARCHIVED', archived_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", id, tenantId);
    }
    public int nextVersion(UUID documentId, UUID tenantId) {
        return jdbc.queryForObject("SELECT COALESCE(MAX(version_number),0)+1 FROM document_versions WHERE document_id=? AND tenant_id=?", Integer.class, documentId, tenantId);
    }

    public DocumentVersion findVersion(UUID tenantId, UUID documentId, UUID versionId) {
        return jdbc.query("SELECT * FROM document_versions WHERE tenant_id=? AND document_id=? AND id=?", this::mapVersion, tenantId, documentId, versionId)
                .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Document version not found in this tenant"));
    }
    public DocumentVersion insertVersion(UUID tenantId, UUID documentId, int version, String ref, String originalFilename, String contentType, long size, String checksum, UUID uploader) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO document_versions(id,tenant_id,document_id,version_number,storage_reference,original_filename,content_type,size_bytes,checksum,uploaded_by) VALUES (?,?,?,?,?,?,?,?,?,?)", id, tenantId, documentId, version, ref, originalFilename, contentType, size, checksum, uploader);
        jdbc.update("UPDATE documents SET current_version_id=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", id, documentId, tenantId);
        return jdbc.queryForObject("SELECT * FROM document_versions WHERE id=?", this::mapVersion, id);
    }
    private Document map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Document(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class), rs.getObject("owner_user_id", UUID.class), rs.getObject("folder_id", UUID.class), rs.getString("name"), rs.getString("description"), DocumentStatus.valueOf(rs.getString("status")), rs.getObject("current_version_id", UUID.class), instant(rs,"created_at"), instant(rs,"updated_at"), instant(rs,"archived_at"));
    }
    private DocumentVersion mapVersion(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new DocumentVersion(rs.getObject("id", UUID.class), rs.getObject("tenant_id", UUID.class), rs.getObject("document_id", UUID.class), rs.getInt("version_number"), rs.getString("storage_reference"), rs.getString("original_filename"), rs.getString("content_type"), rs.getLong("size_bytes"), rs.getString("checksum"), rs.getObject("uploaded_by", UUID.class), instant(rs,"created_at"));
    }
    private Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException { Timestamp t = rs.getTimestamp(column); return t == null ? null : t.toInstant(); }
}
