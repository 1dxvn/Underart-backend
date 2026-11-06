package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSummary;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.FavoritesUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.FavoriteRepositoryPort;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoritesService implements FavoritesUseCase {

    private final FavoriteRepositoryPort favorites;
    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;

    @Override
    public void add(UUID userId, UUID artworkId) {
        ArtworkOwnership.requireExisting(artworkRepository, artworkId);
        favorites.add(userId, artworkId);
    }

    @Override
    public void remove(UUID userId, UUID artworkId) {
        favorites.remove(userId, artworkId);
    }

    @Override
    public PageResult<ArtworkSummary> list(UUID userId, PageQuery page) {
        PageResult<UUID> ids = favorites.artworkIdsByUser(userId, page);
        Map<UUID, Artwork> artworks = artworkRepository.findAllByIds(ids.items()).stream()
                .collect(Collectors.toMap(Artwork::id, Function.identity()));
        Map<UUID, String> covers = mediaRepository.coverUrlsByArtworkIds(ids.items());
        return new PageResult<>(ids.items().stream().map(artworks::get).filter(Objects::nonNull)
                .map(artwork -> new ArtworkSummary(artwork, covers.get(artwork.id()))).toList(),
                ids.page(), ids.size(), ids.totalItems());
    }
}
