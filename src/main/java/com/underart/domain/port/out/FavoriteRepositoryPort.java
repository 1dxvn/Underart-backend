package com.underart.domain.port.out;

import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface FavoriteRepositoryPort {

    void add(UUID userId, UUID artworkId);

    void remove(UUID userId, UUID artworkId);

    boolean exists(UUID userId, UUID artworkId);

    long countByArtwork(UUID artworkId);

    PageResult<UUID> artworkIdsByUser(UUID userId, PageQuery page);
}
