package com.clouddocs.document.api;
import jakarta.validation.constraints.*; import java.util.UUID;
record CreateDocumentRequest(@NotBlank @Size(max=255) String name,@Size(max=2000) String description,UUID folderId) {}
record UpdateDocumentRequest(@NotBlank @Size(max=255) String name,@Size(max=2000) String description,UUID folderId) {}
record CreateVersionRequest(@NotBlank @Size(max=500) String storageReference,@Size(max=255) String mediaType,@PositiveOrZero long sizeBytes,@Size(max=128) String checksum) {}
record CreateFolderRequest(@NotBlank @Size(max=255) String name,UUID parentId) {}
record UpdateFolderRequest(@NotBlank @Size(max=255) String name,UUID parentId) {}
record CreateTagRequest(@NotBlank @Size(max=100) String name) {}
