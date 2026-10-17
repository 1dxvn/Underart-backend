package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.UpdateArtworkUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateArtworkService implements UpdateArtworkUseCase {

    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public Artwork update(UpdateArtworkCommand cmd) {
        Artwork current = ArtworkOwnership.requireOwned(artworkRepository, cmd.artworkId(), cmd.requesterId());
        Artwork updated = new Artwork(current.id(), current.artistId(), cmd.title().trim(),
                cmd.description(), cmd.technique(), cmd.weightKg(), cmd.dimensions(),
                current.suggestedPrice(), current.tags(), current.createdAt());
        return artworkRepository.save(updated);
    }
}
