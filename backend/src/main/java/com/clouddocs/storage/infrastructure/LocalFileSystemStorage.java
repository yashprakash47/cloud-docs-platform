package com.clouddocs.storage.infrastructure;

import com.clouddocs.storage.application.DocumentStorage;
import com.clouddocs.storage.application.StoredObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@ConditionalOnProperty(name = "clouddocs.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileSystemStorage implements DocumentStorage {
    private final Path baseDirectory;

    public LocalFileSystemStorage(@Value("${clouddocs.storage.local-directory:${java.io.tmpdir}/clouddocs-files}") String directory) throws IOException {
        this.baseDirectory = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(baseDirectory);
    }

    @Override
    public StoredObject upload(String storageReference, InputStream content, long expectedSize, String contentType) throws IOException {
        Path destination = resolve(storageReference);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(baseDirectory, "upload-", ".tmp");
        long size = 0;
        MessageDigest digest = sha256();
        try (InputStream input = content; var output = Files.newOutputStream(temporary)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        if (size != expectedSize) {
            Files.deleteIfExists(temporary);
            throw new IOException("Uploaded file size does not match the declared size");
        }
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredObject(storageReference, size, contentType, hex(digest.digest()));
    }

    @Override
    public InputStream download(String storageReference) throws IOException { return Files.newInputStream(resolve(storageReference)); }

    @Override
    public void delete(String storageReference) throws IOException { Files.deleteIfExists(resolve(storageReference)); }

    @Override
    public boolean exists(String storageReference) { return Files.isRegularFile(resolve(storageReference)); }

    private Path resolve(String reference) {
        Path resolved = baseDirectory.resolve(reference).normalize();
        if (!resolved.startsWith(baseDirectory) || resolved.equals(baseDirectory)) throw new IllegalArgumentException("Invalid storage reference");
        return resolved;
    }

    private MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}
