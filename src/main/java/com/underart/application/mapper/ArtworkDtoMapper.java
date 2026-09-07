package com.underart.application.mapper;

import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.CreateArtworkRequest;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.CreateArtworkUseCase;

public final class ArtworkDtoMapper {

    private ArtworkDtoMapper() {}

    public static CreateArtworkUseCase.Command toCommand(CreateArtworkRequest r, Long artistId) {
        return new CreateArtworkUseCase.Command(artistId, r.title(), r.description(), r.imageUrl(),
                r.type(), r.startingPrice(), r.buyNowPrice(), r.endsAt());
    }

    public static ArtworkResponse toResponse(Artwork a) {
        return new ArtworkResponse(a.id(), a.title(), a.description(), a.imageUrl(), a.artistId(), a.createdAt());
    }
}
