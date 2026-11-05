package com.underart.domain.port.in;

import com.underart.domain.model.ArtworkMedia;
import java.util.List;
import java.util.UUID;

public interface ReorderArtworkMediaUseCase {

    List<ArtworkMedia> reorder(UUID artworkId, UUID requesterId, List<UUID> orderedMediaIds);
}
