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
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada: " + artworkId));
    }

    static Artwork requireOwned(ArtworkRepositoryPort repository, UUID artworkId, UUID requesterId) {
        Artwork artwork = requireExisting(repository, artworkId);
        if (!artwork.artistId().equals(requesterId)) {
            throw new ForbiddenOperationException("Solo el autor de la obra puede realizar esta acción");
        }
        return artwork;
    }
}
