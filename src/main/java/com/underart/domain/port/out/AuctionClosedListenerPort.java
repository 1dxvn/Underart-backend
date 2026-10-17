package com.underart.domain.port.out;

import com.underart.domain.model.Auction;

public interface AuctionClosedListenerPort {

    void onAuctionClosed(Auction auction);
}
