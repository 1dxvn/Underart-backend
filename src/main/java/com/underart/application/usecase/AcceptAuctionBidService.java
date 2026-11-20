package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.AcceptAuctionBidUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcceptAuctionBidService implements AcceptAuctionBidUseCase {

    private final AuctionRepositoryPort auctions;
    private final ArtworkRepositoryPort artworks;
    private final BidRepositoryPort bids;
    private final OrderRepositoryPort orders;
    private final OrderFactory orderFactory;
    private final AuctionOutcomeNotifier notifier;
    private final List<AuctionClosedListenerPort> listeners;

    @Override
    @Transactional
    public Order acceptHighest(UUID auctionId, UUID sellerId, UUID expectedBidId) {
        var auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        var artwork = ArtworkOwnership.requireOwned(artworks, auction.artworkId(), sellerId);
        Instant now = Instant.now();
        if (!auction.isActive() || !auction.hasStarted(now) || auction.hasEnded(now)
                || auction.type() == AuctionType.DUTCH || auction.type() == AuctionType.SEALED) {
            throw new BusinessRuleException("This auction does not allow early acceptance");
        }
        if (orders.hasProcessingCheckout(auctionId)) {
            throw new BusinessRuleException("A buyer is completing a payment");
        }
        var bid = bids.findHighestBid(auctionId)
                .orElseThrow(() -> new BusinessRuleException("The auction has no bids"));
        if (!bid.id().equals(expectedBidId)) {
            throw new BusinessRuleException("The leading bid changed. Refresh the auction and try again.");
        }
        var order = orders.save(orderFactory.create(auction, bid.buyerId(), bid.amount(), artwork));
        var closed = auctions.save(auction.withStatus(AuctionStatus.CLOSED));
        orders.expireOpenCheckouts(auctionId);
        listeners.forEach(listener -> listener.onAuctionClosed(closed));
        notifier.sold(artwork, closed, order, false);
        return order;
    }
}
