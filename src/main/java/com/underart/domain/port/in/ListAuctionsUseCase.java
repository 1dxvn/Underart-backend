package com.underart.domain.port.in;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import java.util.List;

public interface ListAuctionsUseCase {

    List<Auction> list(AuctionStatus status);
}
