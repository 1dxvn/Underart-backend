package com.underart.infrastructure.observer;

import com.underart.domain.model.Auction;

public interface BidObserver {

    void onAuctionClosed(Auction auction);
}
