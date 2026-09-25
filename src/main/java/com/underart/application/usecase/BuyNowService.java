package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyNowService implements BuyNowUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final OrderRepositoryPort orderRepository;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public Order buy(UUID auctionId, UUID buyerId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + auctionId));
        if (auction.status() != AuctionStatus.ACTIVE) {
            throw new BusinessRuleException("La subasta no está activa");
        }
        if (auction.immediatePurchasePrice() == null) {
            throw new BusinessRuleException("Esta subasta no tiene compra inmediata");
        }
        BigDecimal subtotal = auction.immediatePurchasePrice();
        BigDecimal shippingCost = new BigDecimal("15.00");
        BigDecimal platformFee = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPaid = Order.calculateTotal(subtotal, shippingCost);
        String trackingNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order(UUID.randomUUID(), auctionId, buyerId, subtotal, shippingCost,
                platformFee, totalPaid, PaymentStatus.PENDING, trackingNumber, Instant.now());
        Order saved = orderRepository.save(order);

        Auction auctionClosed = new Auction(auction.id(), auction.artworkId(), auction.type(),
                AuctionStatus.CLOSED, auction.basePrice(), auction.immediatePurchasePrice(),
                auction.startDate(), auction.endDate());
        auctionRepository.save(auctionClosed);

        activityLogRepository.log("BUY_NOW", "Compra inmediata de subasta " + auctionId, buyerId,
                Map.of("total", totalPaid, "tracking", trackingNumber));
        return saved;
    }
}
