package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkSummary;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface FavoritesUseCase {

    void add(UUID userId, UUID artworkId);

    void remove(UUID userId, UUID artworkId);

    PageResult<ArtworkSummary> list(UUID userId, PageQuery page);
}
