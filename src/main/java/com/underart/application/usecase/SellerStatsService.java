package com.underart.application.usecase;

import com.underart.domain.model.SellerStats;
import com.underart.domain.port.in.SellerStatsUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort.SalesStats;
import com.underart.domain.port.out.OrderRepositoryPort;
import com.underart.domain.port.out.ReviewRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerStatsService implements SellerStatsUseCase {

    private final ArtworkRepositoryPort artworkRepository;
    private final AuctionRepositoryPort auctionRepository;
    private final OrderRepositoryPort orderRepository;
    private final ReviewRepositoryPort reviewRepository;

    @Override
    public SellerStats statsFor(UUID userId) {
        SalesStats sales = orderRepository.salesStatsForSeller(userId);
        return new SellerStats(artworkRepository.countByArtistId(userId),
                auctionRepository.countActiveByArtistId(userId), sales.count(), sales.paidRevenue(),
                orderRepository.countByWinnerId(userId), reviewRepository.summaryForSeller(userId));
    }
}
