package com.underart.application.usecase;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.CloseExpiredAuctionsUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CloseExpiredAuctionsService implements CloseExpiredAuctionsUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final BidRepositoryPort bidRepository;
    private final OrderRepositoryPort orderRepository;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final List<AuctionClosedListenerPort> closedListeners;

    @Override
    public List<UUID> findExpired() {
        return auctionRepository.findExpiredActiveIds(Instant.now());
    }

    @Override
    @Transactional
    public Optional<Order> close(UUID auctionId) {
        Optional<Auction> locked = auctionRepository.findByIdForUpdate(auctionId);
        if (locked.isEmpty() || !locked.get().isActive() || !locked.get().hasEnded(Instant.now())) {
            return Optional.empty();
        }
        Auction auction = locked.get();
        Optional<Bid> winner = bidRepository.findHighestBid(auctionId);
        Optional<Order> order = winner
                .filter(bid -> !orderRepository.existsByAuctionId(auctionId))
                .map(bid -> orderRepository.save(Order.pending(auctionId, bid.buyerId(), bid.amount())));
        Auction closed = auctionRepository.save(auction.withStatus(AuctionStatus.CLOSED));
        closedListeners.forEach(listener -> listener.onAuctionClosed(closed));
        order.ifPresent(o -> activityLogRepository.log("AUCTION_WON",
                "Ganaste la subasta " + auctionId, o.winnerId(),
                Map.of("auctionId", auctionId.toString(), "total", o.totalPaid())));
        return order;
    }
}
