package com.clouddocs.document.api;

import com.clouddocs.document.application.DocumentService;
import com.clouddocs.document.domain.*;
import com.clouddocs.identity.application.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/documents")
public class DocumentController {
    private final AuthenticationService authentication;
    private final DocumentService service;

    public DocumentController(AuthenticationService a, DocumentService s) {
        this.authentication = a;
        this.service = s;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document create(@PathVariable UUID tenantId, @Valid @RequestBody CreateDocumentRequest b, HttpServletRequest r) {
        return service.create(authentication.requireCurrentUser(r), tenantId, b.name(), b.description(), b.folderId());
    }

    @GetMapping("/{id}")
    public Document get(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        return service.get(authentication.requireCurrentUser(r), tenantId, id);
    }

    @GetMapping
    public PageResponse<Document> list(@PathVariable UUID tenantId,
                                       @RequestParam(required = false) DocumentStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       HttpServletRequest r) {
        return service.list(authentication.requireCurrentUser(r), tenantId, status, page, size);
    }

    @PatchMapping("/{id}")
    public Document update(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody UpdateDocumentRequest b, HttpServletRequest r) {
        return service.update(authentication.requireCurrentUser(r), tenantId, id, b.name(), b.description(), b.folderId());
    }

    @PostMapping("/{id}/archive")
    public Document archive(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        return service.archive(authentication.requireCurrentUser(r), tenantId, id);
    }

    @PostMapping("/{id}/trash")
    public Document trash(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        return service.trash(authentication.requireCurrentUser(r), tenantId, id);
    }

    @PostMapping("/{id}/restore")
    public Document restore(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        return service.restore(authentication.requireCurrentUser(r), tenantId, id);
    }

    @PostMapping("/{id}/unarchive")
    public Document unarchive(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        return service.unarchive(authentication.requireCurrentUser(r), tenantId, id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID tenantId, @PathVariable UUID id, HttpServletRequest r) {
        service.deletePermanently(authentication.requireCurrentUser(r), tenantId, id);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentVersion version(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody CreateVersionRequest b, HttpServletRequest r) {
        return service.version(authentication.requireCurrentUser(r), tenantId, id, b.storageReference(),
                b.originalFilename() == null ? "unknown" : b.originalFilename(),
                b.contentType() == null ? "application/octet-stream" : b.contentType(),
                b.checksum(), b.sizeBytes());
    }
}
