package com.underart.application.usecase;

import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindArtworkService implements FindArtworkUseCase {

    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public Artwork findById(UUID id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada: " + id));
    }
}
