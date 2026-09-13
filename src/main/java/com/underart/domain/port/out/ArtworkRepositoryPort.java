package com.underart.domain.port.out;

import com.underart.domain.model.Artwork;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepositoryPort {

    Artwork save(Artwork artwork);

    Optional<Artwork> findById(UUID id);

    List<Artwork> findByArtistId(UUID artistId);
}
