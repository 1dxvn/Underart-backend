package com.underart.application.mapper;

import com.underart.application.dto.ArtworkResponse;
import com.underart.domain.model.Artwork;

public final class ArtworkDtoMapper {

    private ArtworkDtoMapper() {
    }

    public static ArtworkResponse toResponse(Artwork artwork) {
        return ArtworkResponse.from(artwork);
    }
}
