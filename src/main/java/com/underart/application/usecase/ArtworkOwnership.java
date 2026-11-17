package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.UUID;

final class ArtworkOwnership {

    private ArtworkOwnership() {
    }

    static Artwork requireExisting(ArtworkRepositoryPort repository, UUID artworkId) {
        return repository.findById(artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork not found: " + artworkId));
    }

    static Artwork requireOwnedForUpdate(ArtworkRepositoryPort repository, UUID artworkId, UUID requesterId) {
        Artwork artwork = repository.findByIdForUpdate(artworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork not found: " + artworkId));
        if (!artwork.artistId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the artist can modify this artwork");
        }
        return artwork;
    }

    static Artwork requireOwned(ArtworkRepositoryPort repository, UUID artworkId, UUID requesterId) {
        Artwork artwork = requireExisting(repository, artworkId);
        if (!artwork.artistId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the artist can modify this artwork");
        }
        return artwork;
    }
}
