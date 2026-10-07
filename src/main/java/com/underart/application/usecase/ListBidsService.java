package com.underart.application.usecase;

import com.underart.domain.model.Bid;
import com.underart.domain.port.in.ListBidsUseCase;
import com.underart.domain.port.out.BidRepositoryPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBidsService implements ListBidsUseCase {

    private final BidRepositoryPort bidRepository;

    @Override
    public List<Bid> listByAuction(UUID auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }
}
