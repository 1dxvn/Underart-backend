package com.underart.infrastructure.web.controller;

import com.underart.application.dto.OrderResponse;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.in.PayOrderUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final CurrentUserProvider currentUser;
    private final BuyNowUseCase buyNowUseCase;
    private final FindOrderUseCase findOrderUseCase;
    private final PayOrderUseCase payOrderUseCase;

    @PostMapping("/auctions/{id}/buy-now")
    public ResponseEntity<OrderResponse> buyNow(@PathVariable UUID id) {
        Order order = buyNowUseCase.buy(id, currentUser.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(findOrderUseCase.findById(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/pay")
    public ResponseEntity<OrderResponse> pay(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(payOrderUseCase.pay(id, currentUser.currentUserId())));
    }
}
