package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkMedia;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface DirectMediaUploadUseCase {

    UploadTicket requestUpload(UUID artworkId, UUID requesterId, String contentType, long sizeBytes);

    ArtworkMedia confirmUpload(UUID artworkId, UUID requesterId, UUID mediaId, String contentType);

    record UploadTicket(UUID mediaId, String uploadUrl, Instant expiresAt, Map<String, String> requiredHeaders) {
    }
}
