package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CheckoutAuctionSettlement {

    private final AuctionRepositoryPort auctions;
    private final OrderRepositoryPort orders;
    private final ArtworkRepositoryPort artworks;
    private final BidRepositoryPort bids;
    private final AuctionOutcomeNotifier notifier;
    private final List<AuctionClosedListenerPort> listeners;

    Optional<Auction> lockForOrder(UUID orderId) {
        return orders.findCheckoutAuctionId(orderId).map(auctionId -> auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found")));
    }

    void requireOpen(Auction auction, Instant now) {
        if (!auction.isActive() || !auction.hasStarted(now) || auction.hasEnded(now)) {
            throw new BusinessRuleException("The auction is no longer available for this checkout");
        }
        if (orders.hasProcessingCheckout(auction.id())) {
            throw new BusinessRuleException("Another buyer is completing a payment. Try again shortly.");
        }
    }

    void close(Auction auction, Order order) {
        if (auction.type() == AuctionType.DUTCH) {
            bids.save(new Bid(UUID.randomUUID(), auction.id(), order.winnerId(), order.subtotal(), Instant.now()));
        }
        Auction closed = auctions.save(auction.withStatus(AuctionStatus.CLOSED));
        orders.expireOpenCheckouts(auction.id());
        listeners.forEach(listener -> listener.onAuctionClosed(closed));
        notifier.sold(ArtworkOwnership.requireExisting(artworks, auction.artworkId()), closed, order, true);
    }
}
