package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.model.MediaPolicy;
import com.underart.domain.port.in.UploadArtworkMediaUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadArtworkMediaService implements UploadArtworkMediaUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final MediaStoragePort mediaStorage;

    @Override
    public ArtworkMedia upload(UploadMediaCommand cmd) {
        ArtworkOwnership.requireOwned(artworkRepository, cmd.artworkId(), cmd.requesterId());
        MediaKind kind = MediaPolicy.requireAllowed(cmd.contentType(), cmd.sizeBytes(), false);
        long count = mediaRepository.countByArtworkId(cmd.artworkId());
        if (count >= MediaPolicy.MAX_MEDIA_PER_ARTWORK) {
            throw new BusinessRuleException("La obra ya tiene el máximo de " + MediaPolicy.MAX_MEDIA_PER_ARTWORK + " archivos");
        }
        UUID mediaId = UUID.randomUUID();
        String contentType = MediaPolicy.normalize(cmd.contentType());
        String key = MediaPolicy.storageKey(cmd.artworkId(), mediaId, contentType);
        mediaStorage.upload(key, cmd.content(), cmd.sizeBytes(), contentType);
        ArtworkMedia media = new ArtworkMedia(mediaId, cmd.artworkId(), kind, mediaStorage.publicUrl(key),
                key, contentType, cmd.sizeBytes(), (int) count, Instant.now());
        try {
            return mediaRepository.save(media);
        } catch (RuntimeException e) {
            mediaStorage.delete(key);
            throw e;
        }
    }
}
