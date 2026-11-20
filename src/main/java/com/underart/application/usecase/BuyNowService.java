package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import java.math.BigDecimal;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyNowService implements BuyNowUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final OrderRepositoryPort orderRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final OrderFactory orderFactory;

    @Override
    @Transactional
    public Order buy(UUID auctionId, UUID buyerId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        Instant now = Instant.now();
        if (!auction.isActive()) {
            throw new BusinessRuleException("The auction is not active");
        }
        if (!auction.hasStarted(now) || auction.hasEnded(now)) {
            throw new BusinessRuleException("The auction is not in progress");
        }
        BigDecimal purchasePrice = auction.type() == AuctionType.DUTCH
                ? auction.dutchPriceAt(now) : auction.immediatePurchasePrice();
        if (purchasePrice == null) {
            throw new BusinessRuleException("This auction has no buy-now option");
        }
        Artwork artwork = ArtworkOwnership.requireExisting(artworkRepository, auction.artworkId());
        if (artwork.artistId().equals(buyerId)) {
            throw new BusinessRuleException("You cannot buy your own artwork");
        }
        Order saved = orderRepository.save(orderFactory.create(auction, buyerId, purchasePrice, artwork).asCheckout());
        activityLogRepository.log("CHECKOUT_STARTED", "Checkout started for auction " + auctionId, buyerId,
                Map.of("total", saved.totalPaid(), "orderId", saved.id().toString()));
        return saved;
    }
}
