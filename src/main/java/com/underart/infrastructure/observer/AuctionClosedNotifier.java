package com.underart.infrastructure.observer;

import com.underart.domain.model.Auction;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuctionClosedNotifier {

    private static final Logger log = LoggerFactory.getLogger(AuctionClosedNotifier.class);

    private final ActivityLogRepositoryPort activityLog;

    public AuctionClosedNotifier(ActivityLogRepositoryPort activityLog) {
        this.activityLog = activityLog;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onAuctionClosed(Auction auction) {
        String detail = "Subasta %d cerrada por %s".formatted(auction.getId(), auction.getCurrentPrice());
        activityLog.log("AUCTION_CLOSED", auction.getHighestBidderId(), detail);
        log.info("Notificando ganador {} y vendedor {}: {}", auction.getHighestBidderId(), auction.getSellerId(), detail);
    }
}
