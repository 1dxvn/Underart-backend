package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.CreateArtworkRequest;
import com.underart.domain.model.Artwork;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.CreateArtworkUseCase.CreateArtworkCommand;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final CreateArtworkUseCase createArtworkUseCase;
    private final FindArtworkUseCase findArtworkUseCase;
    private final ListArtworksUseCase listArtworksUseCase;

    @PostMapping
    public ResponseEntity<ArtworkResponse> create(@Valid @RequestBody CreateArtworkRequest req) {
        Artwork artwork = createArtworkUseCase.create(new CreateArtworkCommand(
                req.artistId(), req.title(), req.description(), req.technique(),
                req.weightKg(), req.dimensions()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkResponse.from(artwork));
    }

    @GetMapping
    public ResponseEntity<List<ArtworkResponse>> list(@RequestParam(required = false) UUID artistId) {
        List<ArtworkResponse> result = listArtworksUseCase.list(artistId).stream()
                .map(ArtworkResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtworkResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ArtworkResponse.from(findArtworkUseCase.findById(id)));
    }
}
