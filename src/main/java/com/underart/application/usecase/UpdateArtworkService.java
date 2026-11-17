package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.UpdateArtworkUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateArtworkService implements UpdateArtworkUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkEditingPolicy editingPolicy;

    @Override
    @Transactional
    public Artwork update(UpdateArtworkCommand cmd) {
        Artwork current = ArtworkOwnership.requireOwnedForUpdate(artworkRepository, cmd.artworkId(), cmd.requesterId());
        editingPolicy.requireEditable(cmd.artworkId());
        Artwork updated = new Artwork(current.id(), current.artistId(), cmd.title().trim(),
                cmd.description(), cmd.technique(), cmd.weightKg(), cmd.dimensions(),
                current.suggestedPrice(), current.tags(), current.createdAt());
        return artworkRepository.save(updated);
    }
}
