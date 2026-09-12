package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import java.math.BigDecimal;
import java.util.UUID;

public interface CreateArtworkUseCase {

    Artwork create(CreateArtworkCommand cmd);

    record CreateArtworkCommand(
            UUID artistId,
            String title,
            String description,
            String technique,
            BigDecimal weightKg,
            String dimensions) {
    }
}
