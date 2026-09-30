package com.clouddocs.storage.application;

import java.util.UUID;

public final class StorageObjectKeyGenerator {
    private StorageObjectKeyGenerator() {
    }

    public static String documentVersion(UUID tenantId, UUID documentId, UUID versionIdentity) {
        return "tenants/%s/documents/%s/versions/%s/content".formatted(tenantId, documentId, versionIdentity);
    }
}
