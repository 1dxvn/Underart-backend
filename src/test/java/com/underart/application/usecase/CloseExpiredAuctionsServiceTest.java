package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Bid;
import com.underart.domain.model.Order;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.BidRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CloseExpiredAuctionsServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final BidRepositoryPort bids = mock(BidRepositoryPort.class);
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final AuctionClosedListenerPort listener = mock(AuctionClosedListenerPort.class);
    private final CloseExpiredAuctionsService service =
            new CloseExpiredAuctionsService(auctions, bids, orders, log, List.of(listener));

    @BeforeEach
    void setUp() {
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(auctions.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Auction given(Instant end) {
        Auction auction = Fixtures.auction(UUID.randomUUID(), AuctionStatus.ACTIVE, null, end);
        when(auctions.findByIdForUpdate(auction.id())).thenReturn(Optional.of(auction));
        return auction;
    }

    @Test
    void createsOrderForHighestBidderWhenExpired() {
        Auction auction = given(Instant.now().minusSeconds(5));
        UUID winner = UUID.randomUUID();
        when(bids.findHighestBid(auction.id())).thenReturn(Optional.of(
                new Bid(UUID.randomUUID(), auction.id(), winner, new BigDecimal("250.00"), Instant.now())));

        Optional<Order> order = service.close(auction.id());

        assertThat(order).isPresent();
        assertThat(order.get().winnerId()).isEqualTo(winner);
        assertThat(order.get().totalPaid()).isEqualByComparingTo("290.00");
        ArgumentCaptor<Auction> saved = ArgumentCaptor.forClass(Auction.class);
        verify(auctions).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(AuctionStatus.CLOSED);
        verify(listener).onAuctionClosed(saved.getValue());
    }

    @Test
    void closesWithoutOrderWhenThereAreNoBids() {
        Auction auction = given(Instant.now().minusSeconds(5));
        when(bids.findHighestBid(auction.id())).thenReturn(Optional.empty());

        assertThat(service.close(auction.id())).isEmpty();
        verify(orders, never()).save(any());
        verify(auctions).save(any());
    }

    @Test
    void ignoresAuctionThatHasNotEnded() {
        Auction auction = given(Instant.now().plusSeconds(3600));
        assertThat(service.close(auction.id())).isEmpty();
        verify(auctions, never()).save(any());
    }
}
