package com.underart.infrastructure.observer;

import com.underart.domain.model.Bid;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BidObserver {

    private final ActivityLogRepositoryPort activityLog;

    public BidObserver(ActivityLogRepositoryPort activityLog) {
        this.activityLog = activityLog;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onBidPlaced(Bid bid) {
        activityLog.log("BID_PLACED", bid.bidderId(),
                "Puja de %s en subasta %d".formatted(bid.amount(), bid.auctionId()));
    }
}
