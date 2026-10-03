package com.underart.infrastructure.web.controller;

import com.underart.application.dto.OrderResponse;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final BuyNowUseCase buyNowUseCase;

    @PostMapping("/auctions/{id}/buy-now")
    public ResponseEntity<OrderResponse> buyNow(@PathVariable UUID id,
                                                @RequestParam UUID buyerId) {
        Order order = buyNowUseCase.buy(id, buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    // TODO: implementar GET /orders/{id} cuando exista FindOrderUseCase.
}
