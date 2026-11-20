package com.underart.application.usecase;

import com.underart.domain.model.Bid;
import com.underart.domain.model.Auction;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.util.List;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListBidsUseCase;
import com.underart.domain.port.out.BidRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBidsService implements ListBidsUseCase {

    private final BidRepositoryPort bidRepository;
    private final AuctionRepositoryPort auctions;

    @Override
    public PageResult<Bid> listByAuction(UUID auctionId, PageQuery page) {
        if (auctions.findById(auctionId).map(Auction::hasSealedBids).orElse(false)) {
            return new PageResult<>(List.of(), page.page(), page.size(), 0);
        }
        return bidRepository.findByAuctionId(auctionId, page);
    }

    @Override
    public PageResult<Bid> listByBuyer(UUID buyerId, PageQuery page) {
        return bidRepository.findByBuyerId(buyerId, page);
    }
}
