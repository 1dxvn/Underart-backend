package com.underart.application.usecase;

import com.underart.domain.exception.ForbiddenOperationException;
import com.underart.domain.exception.ResourceNotFoundException;
import com.underart.domain.model.Order;
import com.underart.domain.port.in.FindOrderUseCase;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final AuctionRepositoryPort auctionRepository;
    private final ArtworkRepositoryPort artworkRepository;

    @Override
    public Order findById(UUID orderId, UUID requesterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + orderId));
        if (!order.winnerId().equals(requesterId) && !isSeller(order, requesterId)) {
            throw new ForbiddenOperationException("No tienes acceso a esta orden");
        }
        return order;
    }

    private boolean isSeller(Order order, UUID requesterId) {
        return auctionRepository.findById(order.auctionId())
                .flatMap(auction -> artworkRepository.findById(auction.artworkId()))
                .map(artwork -> artwork.artistId().equals(requesterId))
                .orElse(false);
    }
}
