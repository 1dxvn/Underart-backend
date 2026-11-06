package com.underart.application.dto;

import com.underart.domain.port.in.FindArtworkUseCase.ArtworkDetails;
import java.util.List;

public record ArtworkDetailResponse(ArtworkResponse artwork, List<ArtworkMediaResponse> media) {

    public static ArtworkDetailResponse from(ArtworkDetails details) {
        return new ArtworkDetailResponse(ArtworkResponse.from(details.artwork(), details.coverUrl()),
                details.media().stream().map(ArtworkMediaResponse::from).toList());
    }
}
