package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.model.MediaPolicy;
import com.underart.domain.port.in.DirectMediaUploadUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import com.underart.domain.port.out.MediaStoragePort.PresignedUpload;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DirectMediaUploadService implements DirectMediaUploadUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final MediaStoragePort mediaStorage;

    @Override
    public UploadTicket requestUpload(UUID artworkId, UUID requesterId, String contentType, long sizeBytes) {
        ArtworkOwnership.requireOwned(artworkRepository, artworkId, requesterId);
        MediaPolicy.requireAllowed(contentType, sizeBytes, true);
        requireCapacity(artworkId);
        UUID mediaId = UUID.randomUUID();
        String type = MediaPolicy.normalize(contentType);
        PresignedUpload upload = mediaStorage.presignUpload(
                MediaPolicy.storageKey(artworkId, mediaId, type), type, sizeBytes);
        return new UploadTicket(mediaId, upload.uploadUrl(), upload.expiresAt(), upload.requiredHeaders());
    }

    @Override
    public ArtworkMedia confirmUpload(UUID artworkId, UUID requesterId, UUID mediaId, String contentType) {
        ArtworkOwnership.requireOwned(artworkRepository, artworkId, requesterId);
        Optional<ArtworkMedia> existing = mediaRepository.findById(mediaId);
        if (existing.isPresent()) {
            return existing.get();
        }
        String type = MediaPolicy.normalize(contentType);
        String key = MediaPolicy.storageKey(artworkId, mediaId, type);
        long size = mediaStorage.objectSize(key).orElseThrow(() ->
                new BusinessRuleException("No se encontró el archivo subido. Súbelo antes de confirmar"));
        MediaKind kind;
        try {
            kind = MediaPolicy.requireAllowed(type, size, true);
        } catch (BusinessRuleException e) {
            mediaStorage.delete(key);
            throw e;
        }
        long count = requireCapacity(artworkId);
        return mediaRepository.save(new ArtworkMedia(mediaId, artworkId, kind, mediaStorage.publicUrl(key),
                key, type, size, (int) count, Instant.now()));
    }

    private long requireCapacity(UUID artworkId) {
        long count = mediaRepository.countByArtworkId(artworkId);
        if (count >= MediaPolicy.MAX_MEDIA_PER_ARTWORK) {
            throw new BusinessRuleException("La obra ya tiene el máximo de " + MediaPolicy.MAX_MEDIA_PER_ARTWORK + " archivos");
        }
        return count;
    }
}
