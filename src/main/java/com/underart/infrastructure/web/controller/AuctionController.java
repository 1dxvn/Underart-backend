package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.CreateAuctionRequest;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.CancelAuctionUseCase;
import com.underart.domain.port.in.CreateAuctionUseCase;
import com.underart.domain.port.in.CreateAuctionUseCase.CreateAuctionCommand;
import com.underart.domain.port.in.ListAuctionsUseCase;
import jakarta.validation.Valid;
import java.util.List;
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

    private final ListAuctionsUseCase listAuctionsUseCase;
    private final CreateAuctionUseCase createAuctionUseCase;
    private final CancelAuctionUseCase cancelAuctionUseCase;

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> list(
            @RequestParam(required = false) AuctionStatus status) {
        List<Auction> auctions = listAuctionsUseCase.list(status);
        return ResponseEntity.ok(auctions.stream().map(AuctionResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest req) {
        Auction auction = createAuctionUseCase.create(new CreateAuctionCommand(
                req.artworkId(), req.type(), req.basePrice(),
                req.immediatePurchasePrice(), req.startDate(), req.endDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AuctionResponse.from(auction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        cancelAuctionUseCase.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
