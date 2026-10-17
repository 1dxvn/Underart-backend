package com.underart.domain.port.out;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface MediaStoragePort {

    void upload(String key, InputStream content, long sizeBytes, String contentType);

    void delete(String key);

    PresignedUpload presignUpload(String key, String contentType, long sizeBytes);

    Optional<Long> objectSize(String key);

    String publicUrl(String key);

    record PresignedUpload(String uploadUrl, Instant expiresAt, Map<String, String> requiredHeaders) {
    }
}
