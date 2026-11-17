package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.model.MediaPolicy;
import com.underart.domain.port.in.DirectMediaUploadUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort.PresignedUpload;
import com.underart.domain.port.out.MediaStoragePort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectMediaUploadService implements DirectMediaUploadUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkEditingPolicy editingPolicy;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final MediaStoragePort mediaStorage;

    @Override
    @Transactional
    public UploadTicket requestUpload(UUID artworkId, UUID requesterId, String contentType, long sizeBytes) {
        ArtworkOwnership.requireOwnedForUpdate(artworkRepository, artworkId, requesterId);
        editingPolicy.requireEditable(artworkId);
        MediaPolicy.requireAllowed(contentType, sizeBytes, true);
        requireCapacity(artworkId);
        UUID mediaId = UUID.randomUUID();
        String type = MediaPolicy.normalize(contentType);
        PresignedUpload upload = mediaStorage.presignUpload(
                MediaPolicy.storageKey(artworkId, mediaId, type), type, sizeBytes);
        return new UploadTicket(mediaId, upload.uploadUrl(), upload.expiresAt(), upload.requiredHeaders());
    }

    @Override
    @Transactional
    public ArtworkMedia confirmUpload(UUID artworkId, UUID requesterId, UUID mediaId, String contentType) {
        ArtworkOwnership.requireOwnedForUpdate(artworkRepository, artworkId, requesterId);
        editingPolicy.requireEditable(artworkId);
        Optional<ArtworkMedia> existing = mediaRepository.findById(mediaId);
        if (existing.isPresent()) {
            if (!existing.get().artworkId().equals(artworkId)) {
                throw new BusinessRuleException("Media belongs to another artwork");
            }
            return existing.get();
        }
        String type = MediaPolicy.normalize(contentType);
        String key = MediaPolicy.storageKey(artworkId, mediaId, type);
        long size = mediaStorage.objectSize(key).orElseThrow(() ->
                new BusinessRuleException("Uploaded file not found. Upload it before confirming"));
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
            throw new BusinessRuleException("This artwork already has the maximum of " + MediaPolicy.MAX_MEDIA_PER_ARTWORK + " files");
        }
        return count;
    }
}
