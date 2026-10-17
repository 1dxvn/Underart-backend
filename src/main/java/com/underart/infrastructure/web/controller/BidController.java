package com.underart.infrastructure.web.controller;

import com.underart.application.dto.BidRequest;
import com.underart.application.dto.BidResponse;
import com.underart.application.dto.PageResponse;
import com.underart.domain.model.Bid;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.in.ListBidsUseCase;
import com.underart.domain.port.in.PlaceBidUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class BidController {

    private final CurrentUserProvider currentUser;
    private final PlaceBidUseCase placeBidUseCase;
    private final ListBidsUseCase listBidsUseCase;

    @GetMapping("/{id}/bids")
    public ResponseEntity<PageResponse<BidResponse>> list(@PathVariable UUID id,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(
                listBidsUseCase.listByAuction(id, PageQuery.of(page, size)), BidResponse::from));
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<BidResponse> place(@PathVariable UUID id, @Valid @RequestBody BidRequest req) {
        Bid bid = placeBidUseCase.place(id, currentUser.currentUserId(), req.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(BidResponse.from(bid));
    }
}
