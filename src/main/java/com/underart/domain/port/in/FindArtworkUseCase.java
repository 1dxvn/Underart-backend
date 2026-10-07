package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import java.util.UUID;

public interface FindArtworkUseCase {

    Artwork findById(UUID id);
}
