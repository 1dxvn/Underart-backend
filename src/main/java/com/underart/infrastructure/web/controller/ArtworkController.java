package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.CreateArtworkRequest;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.CreateArtworkUseCase.CreateArtworkCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final CreateArtworkUseCase createArtworkUseCase;

    @PostMapping
    public ResponseEntity<ArtworkResponse> create(@Valid @RequestBody CreateArtworkRequest req) {
        Artwork artwork = createArtworkUseCase.create(new CreateArtworkCommand(
                req.artistId(), req.title(), req.description(), req.technique(),
                req.weightKg(), req.dimensions()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkResponse.from(artwork));
    }

    // TODO: implementar GET /{id} cuando exista FindArtworkUseCase.
}
