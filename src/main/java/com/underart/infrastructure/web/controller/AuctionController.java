package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuctionDetailResponse;
import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.CreateAuctionRequest;
import com.underart.application.dto.PageResponse;
import com.underart.application.dto.OrderResponse;
import com.underart.domain.port.in.AcceptAuctionBidUseCase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import com.underart.application.usecase.EditAuctionService;
import org.springframework.web.bind.annotation.PutMapping;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.AuctionSortField;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PriceRange;
import com.underart.domain.port.in.CancelAuctionUseCase;
import com.underart.domain.port.in.CreateAuctionUseCase.CreateAuctionCommand;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.in.FindAuctionUseCase;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import com.underart.infrastructure.web.SortParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auctions", description = "Create, search, view and cancel auctions")
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private static final Map<String, AuctionSortField> SORT_FIELDS = Map.of(
            "endDate", AuctionSortField.END_DATE,
            "startDate", AuctionSortField.START_DATE,
            "basePrice", AuctionSortField.BASE_PRICE);

    private final CurrentUserProvider currentUser;
    private final ListAuctionsUseCase listAuctionsUseCase;
    private final CreateAuctionUseCase createAuctionUseCase;
    private final FindAuctionUseCase findAuctionUseCase;
    private final CancelAuctionUseCase cancelAuctionUseCase;
    private final EditAuctionService editAuction;
    private final AcceptAuctionBidUseCase acceptAuctionBidUseCase;

    @GetMapping
    @Operation(summary = "Search auctions with filters, price range and sorting")
    @SecurityRequirements
    public ResponseEntity<PageResponse<AuctionResponse>> list(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) AuctionType type,
            @RequestParam(required = false) UUID artworkId,
            @RequestParam(required = false) UUID artistId,
            @RequestParam(required = false) Instant endingBefore,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        SortParam<AuctionSortField> order = SortParam.parse(sort, SORT_FIELDS, AuctionSortField.END_DATE, true);
        return ResponseEntity.ok(PageResponse.from(listAuctionsUseCase.list(
                new AuctionSearchCriteria(status, type, artworkId, artistId, endingBefore,
                        new PriceRange(minPrice, maxPrice), order.field(), order.ascending()),
                PageQuery.of(page, size)), AuctionResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Details with current price, highest bid and minimum next bid")
    @SecurityRequirements
    public ResponseEntity<AuctionDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(AuctionDetailResponse.from(findAuctionUseCase.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create an auction for your artwork")
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest req) {
        Auction auction = createAuctionUseCase.create(new CreateAuctionCommand(
                currentUser.currentUserId(), req.artworkId(), req.type(), req.basePrice(),
                req.immediatePurchasePrice(), req.startDate(), req.endDate(), req.currency(), req.floorPrice()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuctionResponse.from(findAuctionUseCase.findById(auction.id()).summary()));
    }

    @PostMapping("/{id}/accept-bid")
    public ResponseEntity<OrderResponse> acceptBid(@PathVariable UUID id, @Valid @RequestBody AcceptBidRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(acceptAuctionBidUseCase.acceptHighest(id, currentUser.currentUserId(), req.bidId())));
    }

    public record AcceptBidRequest(@NotNull UUID bidId) {}

    public record EditAuctionRequest(
            @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal basePrice,
            @Positive @Digits(integer = 12, fraction = 2) BigDecimal immediatePurchasePrice,
            @Positive @Digits(integer = 12, fraction = 2) BigDecimal floorPrice,
            @NotNull @Future Instant endDate) {}

    @PutMapping("/{id}")
    public ResponseEntity<Void> edit(@PathVariable UUID id, @Valid @RequestBody EditAuctionRequest request) {
        editAuction.edit(id, currentUser.currentUserId(), request.basePrice(), request.immediatePurchasePrice(),
                request.floorPrice(), request.endDate());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an active auction without bids (artist only)")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        cancelAuctionUseCase.cancel(id, currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
