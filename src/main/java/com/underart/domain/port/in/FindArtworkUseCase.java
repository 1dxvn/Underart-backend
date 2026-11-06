package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkMedia;
import java.util.List;
import java.util.UUID;

public interface FindArtworkUseCase {

    Artwork findById(UUID id);

    ArtworkDetails findDetails(UUID id);

    record ArtworkDetails(Artwork artwork, String coverUrl, List<ArtworkMedia> media) {
    }
}
