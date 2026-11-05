package com.underart.domain.port.in;

import java.util.UUID;

public interface DeleteArtworkUseCase {

    void delete(UUID artworkId, UUID requesterId);
}
