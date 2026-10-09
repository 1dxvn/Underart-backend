package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.exception.InvalidBidException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.AuctionType;
import com.underart.domain.model.Bid;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceBidServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final BidRepositoryPort bids = mock(BidRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final PlaceBidService service = new PlaceBidService(auctions, bids, log);
    private final UUID auctionId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();

    private Auction auction(AuctionStatus status, Instant end) {
        return new Auction(auctionId, UUID.randomUUID(), AuctionType.SIMPLE, status,
                new BigDecimal("100"), null, Instant.now().minusSeconds(7200), end);
    }

    @BeforeEach
    void noPreviousBids() {
        when(bids.findHighestBid(auctionId)).thenReturn(Optional.empty());
        when(bids.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void rejectsBidOnEndedAuction() {
        when(auctions.findById(auctionId))
                .thenReturn(Optional.of(auction(AuctionStatus.ACTIVE, Instant.now().minusSeconds(60))));

        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("150")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La subasta ya terminó");
        verifyNoInteractions(log);
    }

    @Test
    void rejectsBidBelowBasePrice() {
        when(auctions.findById(auctionId))
                .thenReturn(Optional.of(auction(AuctionStatus.ACTIVE, Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> service.place(auctionId, buyerId, new BigDecimal("50")))
                .isInstanceOf(InvalidBidException.class);
    }

    @Test
    void acceptsValidBidAndLogsIt() {
        when(auctions.findById(auctionId))
                .thenReturn(Optional.of(auction(AuctionStatus.ACTIVE, Instant.now().plusSeconds(3600))));

        Bid bid = service.place(auctionId, buyerId, new BigDecimal("150"));

        assertThat(bid.amount()).isEqualByComparingTo("150");
        verify(log).log(any(), any(), any(), any());
    }
}
