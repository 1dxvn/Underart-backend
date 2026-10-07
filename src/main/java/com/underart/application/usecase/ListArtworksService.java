package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListArtworksService implements ListArtworksUseCase {

    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public List<Artwork> list(UUID artistId) {
        if (artistId == null) {
            return artworkRepository.findAll();
        }
        return artworkRepository.findByArtistId(artistId);
    }
}
