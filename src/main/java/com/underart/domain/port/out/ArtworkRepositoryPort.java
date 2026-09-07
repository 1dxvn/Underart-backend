package com.underart.domain.port.out;

import com.underart.domain.model.Artwork;

import java.util.Optional;

public interface ArtworkRepositoryPort {

    Artwork save(Artwork artwork);

    Optional<Artwork> findById(Long id);
}
