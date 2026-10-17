package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.CreateArtworkRequest;
import com.underart.application.dto.PageResponse;
import com.underart.application.dto.UpdateArtworkRequest;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.CreateArtworkUseCase.CreateArtworkCommand;
import com.underart.domain.port.in.DeleteArtworkUseCase;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.in.UpdateArtworkUseCase;
import com.underart.domain.port.in.UpdateArtworkUseCase.UpdateArtworkCommand;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final CurrentUserProvider currentUser;
    private final CreateArtworkUseCase createArtworkUseCase;
    private final FindArtworkUseCase findArtworkUseCase;
    private final ListArtworksUseCase listArtworksUseCase;
    private final UpdateArtworkUseCase updateArtworkUseCase;
    private final DeleteArtworkUseCase deleteArtworkUseCase;

    @PostMapping
    public ResponseEntity<ArtworkResponse> create(@Valid @RequestBody CreateArtworkRequest req) {
        Artwork artwork = createArtworkUseCase.create(new CreateArtworkCommand(
                currentUser.currentUserId(), req.title(), req.description(), req.technique(),
                req.weightKg(), req.dimensions()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkResponse.from(artwork));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ArtworkResponse>> list(
            @RequestParam(required = false) UUID artistId,
            @RequestParam(required = false) String technique,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(listArtworksUseCase.list(
                new ArtworkSearchCriteria(artistId, technique, tag, q), PageQuery.of(page, size)),
                ArtworkResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtworkResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ArtworkResponse.from(findArtworkUseCase.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArtworkResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateArtworkRequest req) {
        return ResponseEntity.ok(ArtworkResponse.from(updateArtworkUseCase.update(new UpdateArtworkCommand(
                id, currentUser.currentUserId(), req.title(), req.description(), req.technique(),
                req.weightKg(), req.dimensions()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteArtworkUseCase.delete(id, currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
