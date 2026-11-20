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
            String dimensions,
            boolean textValuation) {

        public CreateArtworkCommand(UUID artistId, String title, String description, String technique,
                BigDecimal weightKg, String dimensions) {
            this(artistId, title, description, technique, weightKg, dimensions, true);
        }
    }
}
