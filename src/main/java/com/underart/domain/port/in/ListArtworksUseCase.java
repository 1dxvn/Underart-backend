package com.underart.domain.port.in;

import com.underart.domain.model.Artwork;
import java.util.List;
import java.util.UUID;

public interface ListArtworksUseCase {

    List<Artwork> list(UUID artistId);
}
