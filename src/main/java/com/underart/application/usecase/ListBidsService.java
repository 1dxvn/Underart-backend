package com.underart.application.usecase;

import com.underart.domain.model.Bid;
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

    @Override
    public PageResult<Bid> listByAuction(UUID auctionId, PageQuery page) {
        return bidRepository.findByAuctionId(auctionId, page);
    }

    @Override
    public PageResult<Bid> listByBuyer(UUID buyerId, PageQuery page) {
        return bidRepository.findByBuyerId(buyerId, page);
    }
}
