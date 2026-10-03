package com.underart.infrastructure.web.controller;

import com.underart.application.dto.AuctionResponse;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.ListAuctionsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final ListAuctionsUseCase listAuctionsUseCase;

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> list(
            @RequestParam(required = false) AuctionStatus status) {
        List<Auction> auctions = listAuctionsUseCase.list(status);
        return ResponseEntity.ok(auctions.stream().map(AuctionResponse::from).toList());
    }
}
