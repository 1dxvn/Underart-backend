package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.ArtworkSummary;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListArtworksService implements ListArtworksUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;

    @Override
    public PageResult<ArtworkSummary> list(ArtworkSearchCriteria criteria, PageQuery page) {
        PageResult<Artwork> artworks = artworkRepository.search(criteria, page);
        Map<UUID, String> covers = mediaRepository.coverUrlsByArtworkIds(
                artworks.items().stream().map(Artwork::id).toList());
        return artworks.map(artwork -> new ArtworkSummary(artwork, covers.get(artwork.id())));
    }
}
