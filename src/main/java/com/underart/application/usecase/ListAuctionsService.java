package com.underart.application.usecase;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListAuctionsService implements ListAuctionsUseCase {

    private final AuctionRepositoryPort auctionRepository;

    @Override
    public List<Auction> list(AuctionStatus status) {
        if (status == null) {
            return auctionRepository.findAll();
        }
        return auctionRepository.findByStatus(status);
    }
}
