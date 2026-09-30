package com.clouddocs.storage;

import com.clouddocs.storage.application.StorageObjectKeyGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageObjectKeyGeneratorTest {
    @Test
    void createsDeterministicApplicationOwnedDocumentVersionKey() {
        assertEquals("tenants/00000000-0000-0000-0000-000000000001/documents/00000000-0000-0000-0000-000000000002/versions/00000000-0000-0000-0000-000000000003/content",
                StorageObjectKeyGenerator.documentVersion(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        UUID.fromString("00000000-0000-0000-0000-000000000003")));
    }
}
