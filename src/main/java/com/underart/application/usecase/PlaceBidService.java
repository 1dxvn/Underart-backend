package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidBidException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.AntiSnipingPolicy;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Bid;
import com.underart.domain.model.BidIncrementPolicy;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.port.in.PlaceBidUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.NotificationSenderPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceBidService implements PlaceBidUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final BidRepositoryPort bidRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ActivityLogRepositoryPort activityLogRepository;
    private final NotificationSenderPort notifications;

    @Override
    @Transactional
    public Bid place(UUID auctionId, UUID buyerId, BigDecimal amount) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + auctionId));
        Instant now = Instant.now();
        if (!auction.isActive()) {
            throw new BusinessRuleException("La subasta no está activa");
        }
        if (!auction.hasStarted(now)) {
            throw new BusinessRuleException("La subasta aún no ha comenzado");
        }
        if (auction.hasEnded(now)) {
            throw new BusinessRuleException("La subasta ya terminó");
        }
        Artwork artwork = ArtworkOwnership.requireExisting(artworkRepository, auction.artworkId());
        if (artwork.artistId().equals(buyerId)) {
            throw new BusinessRuleException("No puedes pujar en tu propia obra");
        }
        Optional<Bid> previous = bidRepository.findHighestBid(auctionId);
        BigDecimal highest = previous.map(Bid::amount).orElse(null);
        BigDecimal minimum = BidIncrementPolicy.minimumNextBid(auction.basePrice(), highest);
        if (amount.compareTo(minimum) < 0) {
            throw new InvalidBidException("La puja mínima es " + minimum.toPlainString());
        }
        Bid saved = bidRepository.save(new Bid(UUID.randomUUID(), auctionId, buyerId, amount, now));
        AntiSnipingPolicy.extendedEnd(auction.endDate(), now).ifPresent(newEnd -> {
            auctionRepository.save(auction.withEndDate(newEnd));
            activityLogRepository.log("AUCTION_EXTENDED", "Subasta " + auctionId + " prorrogada por una puja de último momento",
                    buyerId, Map.of("auctionId", auctionId.toString(), "newEndDate", newEnd.toString()));
        });
        previous.filter(bid -> !bid.buyerId().equals(buyerId)).ifPresent(outbid ->
                notifications.send(Notification.create(outbid.buyerId(), NotificationType.OUTBID,
                        "Te superaron en una puja",
                        "Alguien ofreció " + amount.toPlainString() + " por «" + artwork.title()
                                + "». Puja de nuevo antes de que termine.",
                        Map.of("auctionId", auctionId.toString(), "artworkId", artwork.id().toString()))));
        activityLogRepository.log("BID_PLACED", "Puja de " + amount + " en subasta " + auctionId,
                buyerId, Map.of("amount", amount, "auctionId", auctionId.toString()));
        return saved;
    }
}
