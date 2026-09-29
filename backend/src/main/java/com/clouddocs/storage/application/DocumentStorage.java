package com.clouddocs.storage.application;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {
    StoredObject upload(String storageReference, InputStream content, long expectedSize, String contentType) throws IOException;
    InputStream download(String storageReference) throws IOException;
    void delete(String storageReference) throws IOException;
    boolean exists(String storageReference);
}
