package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListArtworksService implements ListArtworksUseCase {

    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public PageResult<Artwork> list(ArtworkSearchCriteria criteria, PageQuery page) {
        return artworkRepository.search(criteria, page);
    }
}
