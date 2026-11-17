package com.underart.application.usecase;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.AuctionSummary;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListAuctionsService implements ListAuctionsUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final AuctionSummaryAssembler summaryAssembler;

    @Override
    public PageResult<AuctionSummary> list(AuctionSearchCriteria criteria, PageQuery page) {
        PageResult<Auction> auctions = auctionRepository.search(criteria, page);
        return new PageResult<>(summaryAssembler.summarize(auctions.items()), auctions.page(), auctions.size(),
                auctions.totalItems());
    }
}
