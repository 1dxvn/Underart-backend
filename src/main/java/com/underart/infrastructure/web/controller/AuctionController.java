package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuctionDetailResponse;
import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.CreateAuctionRequest;
import com.underart.application.dto.PageResponse;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.in.CancelAuctionUseCase;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.in.CreateAuctionUseCase.CreateAuctionCommand;
import com.underart.domain.port.in.FindAuctionUseCase;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.Instant;
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
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final CurrentUserProvider currentUser;
    private final ListAuctionsUseCase listAuctionsUseCase;
    private final CreateAuctionUseCase createAuctionUseCase;
    private final FindAuctionUseCase findAuctionUseCase;
    private final CancelAuctionUseCase cancelAuctionUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<AuctionResponse>> list(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) AuctionType type,
            @RequestParam(required = false) UUID artworkId,
            @RequestParam(required = false) UUID artistId,
            @RequestParam(required = false) Instant endingBefore,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(listAuctionsUseCase.list(
                new AuctionSearchCriteria(status, type, artworkId, artistId, endingBefore),
                PageQuery.of(page, size)), AuctionResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(AuctionDetailResponse.from(findAuctionUseCase.findById(id)));
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest req) {
        Auction auction = createAuctionUseCase.create(new CreateAuctionCommand(
                currentUser.currentUserId(), req.artworkId(), req.type(), req.basePrice(),
                req.immediatePurchasePrice(), req.startDate(), req.endDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AuctionResponse.from(auction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        cancelAuctionUseCase.cancel(id, currentUser.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
