package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkMedia;
import java.io.InputStream;
import java.util.UUID;

public interface UploadArtworkMediaUseCase {

    ArtworkMedia upload(UploadMediaCommand cmd);

    record UploadMediaCommand(
            UUID artworkId,
            UUID requesterId,
            String contentType,
            long sizeBytes,
            InputStream content) {
    }
}
