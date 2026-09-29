package com.clouddocs.storage;

import com.clouddocs.storage.infrastructure.LocalFileSystemStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileSystemStorageTest {
    @Test
    void uploadsDownloadsChecksExistenceChecksumAndDeletes(@TempDir Path directory) throws Exception {
        LocalFileSystemStorage storage = new LocalFileSystemStorage(directory.toString());
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        var stored = storage.upload("tenant/document/version", new java.io.ByteArrayInputStream(content), content.length, "text/plain");
        assertTrue(storage.exists(stored.storageReference()));
        assertEquals("text/plain", stored.contentType());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", stored.checksum());
        assertArrayEquals(content, storage.download(stored.storageReference()).readAllBytes());
        storage.delete(stored.storageReference());
        assertFalse(storage.exists(stored.storageReference()));
    }

    @Test
    void rejectsPathTraversal(@TempDir Path directory) {
        LocalFileSystemStorage storage = assertDoesNotThrow(() -> new LocalFileSystemStorage(directory.toString()));
        assertThrows(IllegalArgumentException.class, () -> storage.exists("../outside"));
    }
}
