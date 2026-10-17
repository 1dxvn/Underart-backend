package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateArtworkUseCase {

    Artwork update(UpdateArtworkCommand cmd);

    record UpdateArtworkCommand(
            UUID artworkId,
            UUID requesterId,
            String title,
            String description,
            String technique,
            BigDecimal weightKg,
            String dimensions) {
    }
}
