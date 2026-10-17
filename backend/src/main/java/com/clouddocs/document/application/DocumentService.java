package com.clouddocs.document.application;

import com.clouddocs.audit.application.AuditService;
import com.clouddocs.audit.domain.AuditAction;
import com.clouddocs.document.domain.*;
import com.clouddocs.document.infrastructure.JdbcDocumentRepository;
import com.clouddocs.identity.domain.User;
import com.clouddocs.operations.api.InvalidRequestException;
import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.tenancy.application.TenantAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final JdbcDocumentRepository repo;
    private final FolderService folders;
    private final TenantAuthorizationService auth;
    private final AuditService audit;
    private final DocumentStorage storage;

    public DocumentService(JdbcDocumentRepository r, FolderService f, TenantAuthorizationService a, AuditService s) {
        this(r, f, a, s, null);
    }

    @Autowired
    public DocumentService(JdbcDocumentRepository r, FolderService f, TenantAuthorizationService a, AuditService s, @Autowired(required = false) DocumentStorage storage) {
        this.repo = r;
        this.folders = f;
        this.auth = a;
        this.audit = s;
        this.storage = storage;
    }

    public Document require(UUID tenant, UUID id) {
        return repo.findByIdAndTenant(id, tenant);
    }

    public DocumentVersion requireVersion(UUID tenant, UUID document, UUID version) {
        require(tenant, document);
        return repo.findVersion(tenant, document, version);
    }

    public DocumentVersion requireVersionForRead(User u, UUID tenant, UUID document, UUID version) {
        auth.requireMembership(u, tenant);
        return requireVersion(tenant, document, version);
    }

    public Document create(User u, UUID tenant, String name, String description, UUID folder) {
        auth.requireWriteAccess(u, tenant);
        if (folder != null) folders.require(tenant, folder);
        Document d = repo.insert(tenant, u.id(), folder, name.trim(), description);
        audit.record(tenant, u.id(), AuditAction.DOCUMENT_CREATED, "DOCUMENT", d.id());
        return d;
    }

    public Document get(User u, UUID tenant, UUID id) {
        auth.requireMembership(u, tenant);
        return require(tenant, id);
    }

    public PageResponse<Document> list(User u, UUID tenant, DocumentStatus status, int page, int size) {
        auth.requireMembership(u, tenant);
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("page must be >= 0 and size must be between 1 and 100");
        }
        return repo.findPage(tenant, status, page, size);
    }

    public Document update(User u, UUID tenant, UUID id, String name, String description, UUID folder) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() == DocumentStatus.ARCHIVED || d.status() == DocumentStatus.TRASH) {
            throw new InvalidRequestException("Archived or trashed documents cannot be updated");
        }
        if (folder != null) folders.require(tenant, folder);
        repo.update(id, tenant, name.trim(), description, folder);
        Document updated = require(tenant, id);
        audit.record(tenant, u.id(), AuditAction.DOCUMENT_UPDATED, "DOCUMENT", id);
        return updated;
    }

    public Document archive(User u, UUID tenant, UUID id) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() != DocumentStatus.ARCHIVED) {
            repo.archive(id, tenant);
            audit.record(tenant, u.id(), AuditAction.DOCUMENT_ARCHIVED, "DOCUMENT", id);
        }
        return require(tenant, id);
    }

    public Document trash(User u, UUID tenant, UUID id) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() != DocumentStatus.TRASH) {
            repo.trash(id, tenant);
            audit.record(tenant, u.id(), AuditAction.DOCUMENT_TRASHED, "DOCUMENT", id);
        }
        return require(tenant, id);
    }

    public Document restore(User u, UUID tenant, UUID id) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() == DocumentStatus.TRASH) {
            repo.restore(id, tenant);
            audit.record(tenant, u.id(), AuditAction.DOCUMENT_RESTORED, "DOCUMENT", id);
        }
        return require(tenant, id);
    }

    public Document unarchive(User u, UUID tenant, UUID id) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() == DocumentStatus.ARCHIVED) {
            repo.unarchive(id, tenant);
            audit.record(tenant, u.id(), AuditAction.DOCUMENT_UNARCHIVED, "DOCUMENT", id);
        }
        return require(tenant, id);
    }

    public void deletePermanently(User u, UUID tenant, UUID id) {
        auth.requireWriteAccess(u, tenant);
        require(tenant, id);
        deletePermanently(tenant, id);
        audit.record(tenant, u.id(), AuditAction.DOCUMENT_DELETED, "DOCUMENT", id);
    }

    public void deletePermanently(UUID tenant, UUID id) {
        List<String> refs = repo.findStorageReferences(tenant, id);
        repo.deletePermanently(tenant, id);
        if (storage != null) {
            for (String ref : refs) {
                if (ref != null && !ref.isBlank()) {
                    try {
                        storage.delete(ref);
                    } catch (Exception e) {
                        log.warn("Failed to delete object {} from storage: {}", ref, e.getMessage());
                    }
                }
            }
        }
    }

    public DocumentVersion version(User u, UUID tenant, UUID id, String ref, String filename, String contentType, String checksum, long size) {
        auth.requireWriteAccess(u, tenant);
        Document d = require(tenant, id);
        if (d.status() == DocumentStatus.ARCHIVED || d.status() == DocumentStatus.TRASH) {
            throw new InvalidRequestException("Archived or trashed documents cannot receive new versions");
        }
        DocumentVersion v = repo.insertVersion(tenant, id, repo.nextVersion(id, tenant), ref, filename, contentType, size, checksum, u.id());
        audit.record(tenant, u.id(), AuditAction.VERSION_CREATED, "DOCUMENT_VERSION", v.id());
        return v;
    }
}
