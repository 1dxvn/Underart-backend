package com.underart.domain.port.out;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepositoryPort {

    Artwork save(Artwork artwork);

    Optional<Artwork> findById(UUID id);

    List<Artwork> findByArtistId(UUID artistId);

    List<Artwork> findAll();

    PageResult<Artwork> search(ArtworkSearchCriteria criteria, PageQuery page);

    void deleteById(UUID id);
}
