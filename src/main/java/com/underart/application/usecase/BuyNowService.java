package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.BuyNowUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
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
    private final List<AuctionClosedListenerPort> closedListeners;

    @Override
    @Transactional
    public Order buy(UUID auctionId, UUID buyerId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + auctionId));
        Instant now = Instant.now();
        if (!auction.isActive()) {
            throw new BusinessRuleException("La subasta no está activa");
        }
        if (!auction.hasStarted(now) || auction.hasEnded(now)) {
            throw new BusinessRuleException("La subasta no está en curso");
        }
        if (auction.immediatePurchasePrice() == null) {
            throw new BusinessRuleException("Esta subasta no tiene compra inmediata");
        }
        Artwork artwork = ArtworkOwnership.requireExisting(artworkRepository, auction.artworkId());
        if (artwork.artistId().equals(buyerId)) {
            throw new BusinessRuleException("No puedes comprar tu propia obra");
        }
        Order saved = orderRepository.save(Order.pending(auctionId, buyerId, auction.immediatePurchasePrice()));
        Auction closed = auctionRepository.save(auction.withStatus(AuctionStatus.CLOSED));
        closedListeners.forEach(listener -> listener.onAuctionClosed(closed));
        activityLogRepository.log("BUY_NOW", "Compra inmediata de subasta " + auctionId, buyerId,
                Map.of("total", saved.totalPaid(), "tracking", saved.trackingNumber()));
        return saved;
    }
}
