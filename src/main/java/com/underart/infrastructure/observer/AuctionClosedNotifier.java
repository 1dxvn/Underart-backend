package com.underart.infrastructure.observer;

import com.underart.domain.model.Auction;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionClosedNotifier implements AuctionClosedListenerPort {

    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public void onAuctionClosed(Auction auction) {
        activityLogRepository.log("AUCTION_CLOSED", "Auction closed: " + auction.id(), null,
                Map.of("artworkId", auction.artworkId().toString(), "status", auction.status().name()));
    }
}
