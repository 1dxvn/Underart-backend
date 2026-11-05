package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.port.in.DeleteArtworkMediaUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.MediaStoragePort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteArtworkMediaService implements DeleteArtworkMediaUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final MediaStoragePort mediaStorage;

    @Override
    public void delete(UUID artworkId, UUID mediaId, UUID requesterId) {
        ArtworkOwnership.requireOwned(artworkRepository, artworkId, requesterId);
        ArtworkMedia media = mediaRepository.findById(mediaId)
                .filter(m -> m.artworkId().equals(artworkId))
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado: " + mediaId));
        mediaStorage.delete(media.storageKey());
        mediaRepository.deleteById(mediaId);
        List<ArtworkMedia> remaining = mediaRepository.findByArtworkId(artworkId);
        List<ArtworkMedia> compacted = new ArrayList<>();
        for (int i = 0; i < remaining.size(); i++) {
            compacted.add(remaining.get(i).withPosition(i));
        }
        mediaRepository.saveAll(compacted);
    }
}
