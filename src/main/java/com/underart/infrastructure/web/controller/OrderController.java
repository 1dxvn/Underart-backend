package com.underart.infrastructure.web.controller;

import com.underart.application.dto.CreateReviewRequest;
import com.underart.application.dto.OrderResponse;
import com.underart.application.usecase.PaymentTransactions;
import com.underart.application.dto.ReviewResponse;
import com.underart.application.dto.ShipOrderRequest;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.in.OrderFulfillmentUseCase;
import com.underart.domain.port.in.PayOrderUseCase;
import com.underart.domain.port.in.ReviewsUseCase;
import com.underart.infrastructure.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.underart.domain.port.out.PaymentGatewayPort.CheckoutCard;
import java.util.UUID;
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
@Tag(name = "Orders", description = "Buy now, payment, shipping, delivery and reviews")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final CurrentUserProvider currentUser;
    private final PaymentTransactions paymentTransactions;
    private final BuyNowUseCase buyNowUseCase;
    private final FindOrderUseCase findOrderUseCase;
    private final PayOrderUseCase payOrderUseCase;
    private final OrderFulfillmentUseCase orderFulfillmentUseCase;
    private final ReviewsUseCase reviewsUseCase;

    @PostMapping("/auctions/{id}/buy-now")
    @Operation(summary = "Buy at the buy-now price")
    public ResponseEntity<OrderResponse> buyNow(@PathVariable UUID id) {
        Order order = buyNowUseCase.buy(id, currentUser.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Order details (buyer or seller)")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(findOrderUseCase.findById(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/cancel-checkout")
    public ResponseEntity<OrderResponse> cancelCheckout(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(paymentTransactions.cancelCheckout(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/pay")
    @Operation(summary = "Pay an order with the registered payment method")
    public ResponseEntity<OrderResponse> pay(@PathVariable UUID id, @Valid @RequestBody(required = false) TestCardRequest request) {
        UUID buyer = currentUser.currentUserId();
        Order order = request == null ? payOrderUseCase.pay(id, buyer)
                : payOrderUseCase.pay(id, buyer, new CheckoutCard(
                        request.token(), request.method(), request.issuer(), request.installments()));
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    public record TestCardRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = "[a-zA-Z0-9_-]+") String token,
            @NotBlank @Size(max = 30) @Pattern(regexp = "[a-zA-Z0-9_-]+") String method,
            @Size(max = 15) @Pattern(regexp = "[0-9]*") String issuer,
            @Min(1) @Max(12) int installments) {}

    @PostMapping("/orders/{id}/ship")
    @Operation(summary = "Mark as shipped (seller)")
    public ResponseEntity<OrderResponse> ship(@PathVariable UUID id, @Valid @RequestBody ShipOrderRequest req) {
        return ResponseEntity.ok(OrderResponse.from(orderFulfillmentUseCase.ship(id, currentUser.currentUserId(),
                req.carrier(), req.trackingNumber())));
    }

    @PostMapping("/orders/{id}/confirm-delivery")
    @Operation(summary = "Confirm delivery (buyer)")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(
                orderFulfillmentUseCase.confirmDelivery(id, currentUser.currentUserId())));
    }

    @PostMapping("/orders/{id}/review")
    @Operation(summary = "Review a seller after delivery")
    public ResponseEntity<ReviewResponse> review(@PathVariable UUID id, @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(
                reviewsUseCase.create(id, currentUser.currentUserId(), req.rating(), req.comment())));
    }
}
