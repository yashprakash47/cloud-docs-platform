package com.clouddocs.storage;

import com.clouddocs.storage.infrastructure.S3DocumentStorage;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3DocumentStorageTest {
    @Test
    void delegatesObjectOperationsAndCalculatesChecksum() throws Exception {
        S3Client client = mock(S3Client.class);
        S3DocumentStorage storage = new S3DocumentStorage(client, "private-bucket");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        when(client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(software.amazon.awssdk.services.s3.model.HeadObjectResponse.builder().build());
        when(client.getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(content))));

        var stored = storage.upload("tenants/t/documents/d/versions/v/content", new ByteArrayInputStream(content), content.length, "text/plain");
        assertNotNull(stored.checksum());
        assertTrue(storage.exists(stored.storageReference()));
        assertArrayEquals(content, storage.download(stored.storageReference()).readAllBytes());
        storage.delete(stored.storageReference());

        verify(client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(client).headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class));
        verify(client).getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class));
        verify(client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
}
