package com.underart.application.usecase;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.port.in.CancelAuctionUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelAuctionService implements CancelAuctionUseCase {

    private final AuctionRepositoryPort auctionRepository;
    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public void cancel(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada: " + auctionId));
        if (auction.status() != AuctionStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se pueden cancelar subastas activas");
        }
        Auction auctionCancelled = new Auction(auction.id(), auction.artworkId(), auction.type(),
                AuctionStatus.CANCELLED, auction.basePrice(), auction.immediatePurchasePrice(),
                auction.startDate(), auction.endDate());
        auctionRepository.save(auctionCancelled);
        activityLogRepository.log("AUCTION_CANCELLED", "Subasta cancelada: " + auctionId, null, Map.of());
    }
}
