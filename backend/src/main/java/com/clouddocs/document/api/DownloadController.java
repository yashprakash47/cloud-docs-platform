package com.clouddocs.document.api;

import com.clouddocs.document.application.DocumentService;
import com.clouddocs.document.domain.DocumentVersion;
import com.clouddocs.identity.application.AuthenticationService;
import com.clouddocs.operations.api.ResourceNotFoundException;
import com.clouddocs.storage.application.DocumentStorage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/documents/{documentId}/versions/{versionId}/content")
public class DownloadController {
    private final AuthenticationService authentication;
    private final DocumentService documents;
    private final DocumentStorage storage;

    public DownloadController(AuthenticationService authentication, DocumentService documents, DocumentStorage storage) {
        this.authentication = authentication;
        this.documents = documents;
        this.storage = storage;
    }

    @GetMapping
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID tenantId, @PathVariable UUID documentId,
                                                        @PathVariable UUID versionId, HttpServletRequest request) {
        DocumentVersion version = documents.requireVersionForRead(authentication.requireCurrentUser(request), tenantId, documentId, versionId);
        if (!storage.exists(version.storageReference())) throw new ResourceNotFoundException("Stored document content was not found");
        try {
            ContentDisposition disposition = ContentDisposition.attachment().filename(version.originalFilename()).build();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(version.contentType()))
                    .contentLength(version.sizeBytes()).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(new InputStreamResource(storage.download(version.storageReference())));
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResourceNotFoundException("Stored document content was not found");
        }
    }
}
