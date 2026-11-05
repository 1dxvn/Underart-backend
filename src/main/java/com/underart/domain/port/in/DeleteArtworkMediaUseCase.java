package com.underart.domain.port.in;

import java.util.UUID;

public interface DeleteArtworkMediaUseCase {

    void delete(UUID artworkId, UUID mediaId, UUID requesterId);
}
