package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.model.MediaKind;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.FavoriteRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindArtworkService implements FindArtworkUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;
    private final FavoriteRepositoryPort favorites;

    @Override
    public Artwork findById(UUID id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artwork not found: " + id));
    }

    @Override
    public ArtworkDetails findDetails(UUID id, UUID viewerId) {
        Artwork artwork = findById(id);
        List<ArtworkMedia> media = mediaRepository.findByArtworkId(id);
        String cover = media.stream().filter(m -> m.kind() == MediaKind.IMAGE).map(ArtworkMedia::url)
                .findFirst().orElse(null);
        return new ArtworkDetails(artwork, cover, media, favorites.countByArtwork(id),
                viewerId != null && favorites.exists(viewerId, id));
    }
}
