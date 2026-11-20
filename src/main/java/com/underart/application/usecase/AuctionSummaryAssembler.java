package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionType;
import java.time.Instant;
import com.underart.domain.model.AuctionSummary;
import com.underart.domain.port.out.ArtworkMediaRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort.BidStats;
import com.underart.domain.port.out.BidRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionSummaryAssembler {

    private static final BidStats NO_BIDS = new BidStats(null, 0);

    private final BidRepositoryPort bidRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ArtworkMediaRepositoryPort mediaRepository;

    public List<AuctionSummary> summarize(List<Auction> auctions) {
        if (auctions.isEmpty()) {
            return List.of();
        }
        Set<UUID> artworkIds = auctions.stream().map(Auction::artworkId).collect(Collectors.toSet());
        Map<UUID, BidStats> stats = bidRepository.statsByAuctionIds(auctions.stream().map(Auction::id).toList());
        Map<UUID, String> titles = artworkRepository.findAllByIds(artworkIds).stream()
                .collect(Collectors.toMap(Artwork::id, Artwork::title));
        Map<UUID, String> covers = mediaRepository.coverUrlsByArtworkIds(artworkIds);
        Instant now = Instant.now();
        return auctions.stream().map(auction -> {
            BidStats bidStats = stats.getOrDefault(auction.id(), NO_BIDS);
            return new AuctionSummary(auction, titles.get(auction.artworkId()), covers.get(auction.artworkId()),
                    auction.type() == AuctionType.DUTCH && auction.isActive() ? auction.dutchPriceAt(now)
                            : auction.hasSealedBids() || bidStats.highestAmount() == null
                                    ? auction.basePrice() : bidStats.highestAmount(),
                    bidStats.count());
        }).toList();
    }

    public AuctionSummary summarize(Auction auction) {
        return summarize(List.of(auction)).get(0);
    }
}
