package com.clouddocs.storage;

import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.storage.infrastructure.LocalFileSystemStorage;
import com.clouddocs.storage.infrastructure.S3DocumentStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
@ActiveProfiles("test")
class StorageProviderSelectionTest {
    @Autowired DocumentStorage storage;

    @Test
    void localIsTheDefaultProvider() {
        assertInstanceOf(LocalFileSystemStorage.class, storage);
    }
}

@SpringBootTest(properties = {"clouddocs.storage.provider=s3", "clouddocs.storage.s3.bucket=test-bucket"})
@ActiveProfiles("test")
class S3ProviderSelectionTest {
    @Autowired DocumentStorage storage;

    @Test
    void s3ProviderSelectsS3AdapterWithoutCallingAWS() {
        assertInstanceOf(S3DocumentStorage.class, storage);
    }
}
