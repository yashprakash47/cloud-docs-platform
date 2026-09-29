package com.clouddocs.document.application;

import com.clouddocs.document.domain.DocumentVersion;
import com.clouddocs.identity.domain.User;
import com.clouddocs.operations.api.InvalidRequestException;
import com.clouddocs.operations.api.ResourceNotFoundException;
import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.storage.application.StoredObject;
import com.clouddocs.tenancy.application.TenantAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadIntentService {
    private final DocumentService documents;
    private final TenantAuthorizationService authorization;
    private final DocumentStorage storage;
    private final long maxFileSize;
    private final Set<String> allowedContentTypes;
    private final ConcurrentHashMap<UUID, PendingUpload> intents = new ConcurrentHashMap<>();

    public UploadIntentService(DocumentService documents, TenantAuthorizationService authorization, DocumentStorage storage,
                               @Value("${clouddocs.storage.max-file-size-bytes:10485760}") long maxFileSize,
                               @Value("${clouddocs.storage.allowed-content-types:text/plain,application/pdf,image/png}") String allowedContentTypes) {
        this.documents = documents;
        this.authorization = authorization;
        this.storage = storage;
        this.maxFileSize = maxFileSize;
        this.allowedContentTypes = Arrays.stream(allowedContentTypes.split(",")).map(String::trim).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public UploadIntent create(User user, UUID tenant, UUID document, String filename, String contentType, long size) {
        authorization.requireWriteAccess(user, tenant);
        documents.require(tenant, document);
        validate(filename, contentType, size);
        UUID id = UUID.randomUUID();
        String reference = tenant + "/" + document + "/" + id;
        intents.put(id, new PendingUpload(id, user.id(), tenant, document, filename, contentType, size, reference));
        return new UploadIntent(id, reference, maxFileSize);
    }

    public void upload(User user, UUID intentId, MultipartFile file) {
        PendingUpload intent = requireOwned(user, intentId);
        validate(file.getOriginalFilename(), file.getContentType(), file.getSize());
        if (!intent.originalFilename.equals(file.getOriginalFilename()) || !intent.contentType.equals(file.getContentType()) || intent.expectedSize != file.getSize()) {
            throw new InvalidRequestException("Uploaded file does not match the upload intent");
        }
        try {
            intent.stored = storage.upload(intent.storageReference, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException exception) {
            throw new InvalidRequestException("The file could not be stored");
        }
    }

    public DocumentVersion complete(User user, UUID intentId) {
        PendingUpload intent = requireOwned(user, intentId);
        if (intent.stored == null) throw new InvalidRequestException("Upload content is incomplete");
        StoredObject object = intent.stored;
        DocumentVersion version = documents.version(user, intent.tenant, intent.document, object.storageReference(), intent.originalFilename, object.contentType(), object.checksum(), object.sizeBytes());
        intents.remove(intentId);
        return version;
    }

    private PendingUpload requireOwned(User user, UUID id) {
        PendingUpload intent = intents.get(id);
        if (intent == null || !intent.userId.equals(user.id())) throw new ResourceNotFoundException("Upload intent not found");
        authorization.requireMembership(user, intent.tenant);
        return intent;
    }

    private void validate(String filename, String contentType, long size) {
        if (filename == null || filename.isBlank()) throw new InvalidRequestException("A filename is required");
        if (size <= 0) throw new InvalidRequestException("Empty files are not allowed");
        if (size > maxFileSize) throw new InvalidRequestException("File exceeds the configured maximum size");
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) throw new InvalidRequestException("Content type is not allowed");
    }

    public record UploadIntent(UUID id, String storageReference, long maxFileSize) {}

    private static final class PendingUpload {
        private final UUID id, userId, tenant, document;
        private final String originalFilename, contentType, storageReference;
        private final long expectedSize;
        private StoredObject stored;
        private PendingUpload(UUID id, UUID userId, UUID tenant, UUID document, String filename, String contentType, long size, String reference) {
            this.id = id; this.userId = userId; this.tenant = tenant; this.document = document; this.originalFilename = filename; this.contentType = contentType; this.expectedSize = size; this.storageReference = reference;
        }
    }
}
