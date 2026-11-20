package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidBidException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.AntiSnipingPolicy;
import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.Bid;
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
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        Instant now = Instant.now();
        if (!auction.isActive()) {
            throw new BusinessRuleException("The auction is not active");
        }
        if (!auction.hasStarted(now)) {
            throw new BusinessRuleException("The auction has not started");
        }
        if (auction.hasEnded(now)) {
            throw new BusinessRuleException("The auction has ended");
        }
        Artwork artwork = ArtworkOwnership.requireExisting(artworkRepository, auction.artworkId());
        if (artwork.artistId().equals(buyerId)) {
            throw new BusinessRuleException("You cannot bid on your own artwork");
        }
        if (auction.type() == AuctionType.DUTCH) {
            throw new BusinessRuleException("Accept the current Dutch auction price to purchase the artwork");
        }
        if (!auction.validPriceStep(amount)) {
            throw new InvalidBidException("COP bids must be multiples of 1,000");
        }
        if (auction.type() == AuctionType.SEALED) {
            if (amount.compareTo(auction.basePrice()) < 0) {
                throw new InvalidBidException("The sealed bid must meet the base price");
            }
            if (bidRepository.existsByAuctionIdAndBuyerId(auctionId, buyerId)) {
                throw new BusinessRuleException("You already submitted your sealed bid");
            }
            return bidRepository.save(new Bid(UUID.randomUUID(), auctionId, buyerId, amount, now));
        }
        Optional<Bid> previous = bidRepository.findHighestBid(auctionId);
        BigDecimal highest = previous.map(Bid::amount).orElse(null);
        BigDecimal minimum = auction.minimumBid(highest);
        if (amount.compareTo(minimum) < 0) {
            throw new InvalidBidException("The minimum bid is " + minimum.toPlainString());
        }
        Bid saved = bidRepository.save(new Bid(UUID.randomUUID(), auctionId, buyerId, amount, now));
        AntiSnipingPolicy.extendedEnd(auction.endDate(), now).ifPresent(newEnd -> {
            auctionRepository.save(auction.withEndDate(newEnd));
            activityLogRepository.log("AUCTION_EXTENDED", "Auction " + auctionId + " extended by a last-minute bid",
                    buyerId, Map.of("auctionId", auctionId.toString(), "newEndDate", newEnd.toString()));
        });
        previous.filter(bid -> !bid.buyerId().equals(buyerId)).ifPresent(outbid ->
                notifications.send(Notification.create(outbid.buyerId(), NotificationType.OUTBID,
                        "You have been outbid",
                        "Someone bid " + amount.toPlainString() + " for «" + artwork.title()
                                + "». Bid again before the auction ends.",
                        Map.of("auctionId", auctionId.toString(), "artworkId", artwork.id().toString()))));
        activityLogRepository.log("BID_PLACED", "Bid of " + amount + " on auction " + auctionId,
                buyerId, Map.of("amount", amount, "auctionId", auctionId.toString()));
        return saved;
    }
}
