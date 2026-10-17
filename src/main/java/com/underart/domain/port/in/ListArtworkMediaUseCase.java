package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkMedia;
import java.util.List;
import java.util.UUID;

public interface ListArtworkMediaUseCase {

    List<ArtworkMedia> list(UUID artworkId);
}
