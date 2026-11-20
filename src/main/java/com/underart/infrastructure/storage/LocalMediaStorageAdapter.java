package com.underart.infrastructure.storage;

import com.underart.domain.exception.MediaStorageException;
import com.underart.domain.port.out.MediaStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "local")
public class LocalMediaStorageAdapter implements MediaStoragePort {

    private static final Pattern KEY = Pattern.compile(
            "(?:artworks|avatars)/[0-9a-f-]{36}/[0-9a-f-]{36}\\.(jpg|png|webp|mp4|webm|mov)");
    private final Path root;

    public LocalMediaStorageAdapter(@Value("${storage.local.directory}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void upload(String key, InputStream content, long sizeBytes, String contentType) {
        Path destination = resolve(key);
        try {
            Files.createDirectories(destination.getParent());
            Path staging = Files.createDirectories(root.resolve(".staging"));
            Path temporary = Files.createTempFile(staging, "upload-", ".tmp");
            try {
                Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new MediaStorageException("Could not save the file locally", e);
        }
    }

    @Override
    public byte[] readPrefix(String key, int maxBytes) {
        try (InputStream content = Files.newInputStream(resolve(key))) {
            return content.readNBytes(maxBytes);
        } catch (IOException e) {
            throw new MediaStorageException("Could not read the local file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new MediaStorageException("Could not delete the local file", e);
        }
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long sizeBytes) {
        throw new MediaStorageException("Direct upload requires R2. Use multipart upload for local storage.");
    }

    @Override
    public Optional<Long> objectSize(String key) {
        try {
            return Optional.of(Files.size(resolve(key)));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new MediaStorageException("Could not read the local file", e);
        }
    }

    @Override
    public String publicUrl(String key) {
        resolve(key);
        return "/media/" + key;
    }

    private Path resolve(String key) {
        if (!KEY.matcher(key).matches()) throw new MediaStorageException("Invalid media key");
        return root.resolve(key);
    }
}
