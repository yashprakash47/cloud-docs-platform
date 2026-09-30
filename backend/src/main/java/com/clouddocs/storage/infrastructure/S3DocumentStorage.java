package com.clouddocs.storage.infrastructure;

import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.storage.application.StoredObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@ConditionalOnProperty(name = "clouddocs.storage.provider", havingValue = "s3")
public class S3DocumentStorage implements DocumentStorage {
    private final S3Client s3;
    private final String bucket;

    @Autowired
    public S3DocumentStorage(@Value("${clouddocs.storage.s3.bucket}") String bucket,
                             @Value("${clouddocs.storage.s3.region:${AWS_REGION:ap-south-1}}") String region) {
        if (bucket == null || bucket.isBlank()) throw new IllegalStateException("CLOUDDOCS_S3_BUCKET is required when S3 storage is enabled");
        this.bucket = bucket;
        this.s3 = S3Client.builder().region(Region.of(region)).build();
    }

    public S3DocumentStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public StoredObject upload(String storageReference, InputStream content, long expectedSize, String contentType) throws IOException {
        MessageDigest digest = sha256();
        try (DigestInputStream digestInput = new DigestInputStream(content, digest)) {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(storageReference).contentType(contentType).build(),
                    RequestBody.fromInputStream(digestInput, expectedSize));
        } catch (RuntimeException exception) {
            throw new IOException("S3 upload failed", exception);
        }
        return new StoredObject(storageReference, expectedSize, contentType, hex(digest.digest()));
    }

    @Override
    public InputStream download(String storageReference) throws IOException {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(storageReference).build());
        } catch (RuntimeException exception) {
            throw new IOException("S3 download failed", exception);
        }
    }

    @Override
    public void delete(String storageReference) throws IOException {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageReference).build());
        } catch (RuntimeException exception) {
            throw new IOException("S3 delete failed", exception);
        }
    }

    @Override
    public boolean exists(String storageReference) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageReference).build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) return false;
            throw exception;
        }
    }

    private MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}
