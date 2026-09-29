package com.clouddocs.storage.application;

public record StoredObject(String storageReference, long sizeBytes, String contentType, String checksum) {
}
