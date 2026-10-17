package com.underart.application.usecase;

import com.underart.domain.model.ArtworkMedia;
import com.underart.domain.port.in.ListArtworkMediaUseCase;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListArtworkMediaService implements ListArtworkMediaUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;

    @Override
    public List<ArtworkMedia> list(UUID artworkId) {
        ArtworkOwnership.requireExisting(artworkRepository, artworkId);
        return mediaRepository.findByArtworkId(artworkId);
    }
}
