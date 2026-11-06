package com.underart.infrastructure.web.controller;

import com.underart.application.dto.CreateReviewRequest;
import com.underart.application.dto.OrderResponse;
import com.underart.application.dto.ReviewResponse;
import com.underart.application.dto.ShipOrderRequest;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.in.OrderFulfillmentUseCase;
import com.underart.domain.port.in.PayOrderUseCase;
import com.underart.domain.port.in.ReviewsUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Órdenes", description = "Compra inmediata, pago, envío, entrega y reseñas")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final CurrentUserProvider currentUser;
    private final BuyNowUseCase buyNowUseCase;
    private final FindOrderUseCase findOrderUseCase;
    private final PayOrderUseCase payOrderUseCase;
    private final OrderFulfillmentUseCase orderFulfillmentUseCase;
    private final ReviewsUseCase reviewsUseCase;

    @PostMapping("/auctions/{id}/buy-now")
    @Operation(summary = "Comprar ya al precio de compra inmediata")
    public ResponseEntity<OrderResponse> buyNow(@PathVariable UUID id) {
        Order order = buyNowUseCase.buy(id, currentUser.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Detalle de una orden (comprador o vendedor)")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(findOrderUseCase.findById(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/pay")
    @Operation(summary = "Pagar la orden con el método de pago registrado")
    public ResponseEntity<OrderResponse> pay(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(payOrderUseCase.pay(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/ship")
    @Operation(summary = "Marcar como enviada (vendedor)")
    public ResponseEntity<OrderResponse> ship(@PathVariable UUID id, @Valid @RequestBody ShipOrderRequest req) {
        return ResponseEntity.ok(OrderResponse.from(orderFulfillmentUseCase.ship(id, currentUser.currentUserId(),
                req.carrier(), req.trackingNumber())));
    }

    @PostMapping("/orders/{id}/confirm-delivery")
    @Operation(summary = "Confirmar la entrega (comprador)")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(
                orderFulfillmentUseCase.confirmDelivery(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/review")
    @Operation(summary = "Reseñar al vendedor tras la entrega")
    public ResponseEntity<ReviewResponse> review(@PathVariable UUID id, @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(
                reviewsUseCase.create(id, currentUser.currentUserId(), req.rating(), req.comment())));
    }
}
