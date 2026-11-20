package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ArtworkDetailResponse;
import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.CreateArtworkRequest;
import com.underart.application.dto.PageResponse;
import com.underart.application.dto.UpdateArtworkRequest;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.ArtworkSortField;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PriceRange;
import com.underart.domain.port.in.CreateArtworkUseCase.CreateArtworkCommand;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.DeleteArtworkUseCase;
import com.underart.domain.port.in.FavoritesUseCase;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.in.UpdateArtworkUseCase.UpdateArtworkCommand;
import com.underart.domain.port.in.UpdateArtworkUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import com.underart.infrastructure.web.SortParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
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
@Tag(name = "Artworks", description = "Artwork catalog, search, editing and favorites")
@RequestMapping("/api/v1/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private static final Map<String, ArtworkSortField> SORT_FIELDS = Map.of(
            "createdAt", ArtworkSortField.CREATED_AT,
            "suggestedPrice", ArtworkSortField.SUGGESTED_PRICE,
            "title", ArtworkSortField.TITLE);

    private final CurrentUserProvider currentUser;
    private final CreateArtworkUseCase createArtworkUseCase;
    private final FindArtworkUseCase findArtworkUseCase;
    private final ListArtworksUseCase listArtworksUseCase;
    private final UpdateArtworkUseCase updateArtworkUseCase;
    private final DeleteArtworkUseCase deleteArtworkUseCase;
    private final FavoritesUseCase favoritesUseCase;

    @PostMapping
    @Operation(summary = "Create artwork with optional AI suggestions")
    public ResponseEntity<ArtworkResponse> create(@Valid @RequestBody CreateArtworkRequest req) {
        Artwork artwork = createArtworkUseCase.create(new CreateArtworkCommand(
                currentUser.currentUserId(), req.title(), req.description(), req.technique(),
                req.weightKg(), req.dimensions(), !Boolean.FALSE.equals(req.textValuation())));
        return ResponseEntity.status(HttpStatus.CREATED).body(ArtworkResponse.from(artwork));
    }

    @GetMapping
    @Operation(summary = "Search artworks with filters, price range and sorting")
    @SecurityRequirements
    public ResponseEntity<PageResponse<ArtworkResponse>> list(
            @RequestParam(required = false) UUID artistId,
            @RequestParam(required = false) String technique,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        SortParam<ArtworkSortField> order = SortParam.parse(sort, SORT_FIELDS, ArtworkSortField.CREATED_AT, false);
        return ResponseEntity.ok(PageResponse.from(listArtworksUseCase.list(
                new ArtworkSearchCriteria(artistId, technique, tag, q, new PriceRange(minPrice, maxPrice),
                        order.field(), order.ascending()), PageQuery.of(page, size)),
                ArtworkResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Artwork details with gallery and favorites")
    @SecurityRequirements
    public ResponseEntity<ArtworkDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ArtworkDetailResponse.from(
                findArtworkUseCase.findDetails(id, currentUser.currentUserIdIfPresent().orElse(null))));
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Add artwork to favorites")
    public ResponseEntity<Void> addFavorite(@PathVariable UUID id) {
        favoritesUseCase.add(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "Remove artwork from favorites")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID id) {
        favoritesUseCase.remove(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit artwork (artist only)")
    public ResponseEntity<ArtworkResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateArtworkRequest req) {
        updateArtworkUseCase.update(new UpdateArtworkCommand(id, currentUser.currentUserId(), req.title(),
                req.description(), req.technique(), req.weightKg(), req.dimensions()));
        FindArtworkUseCase.ArtworkDetails details = findArtworkUseCase.findDetails(id, currentUser.currentUserId());
        return ResponseEntity.ok(ArtworkResponse.from(details.artwork(), details.coverUrl()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete artwork without auctions (artist only)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteArtworkUseCase.delete(id, currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
