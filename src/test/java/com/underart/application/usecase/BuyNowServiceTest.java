package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.underart.domain.exception.BusinessRuleException;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Order;
import com.underart.domain.model.PaymentStatus;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import com.underart.domain.port.out.ArtworkRepositoryPort;
import com.underart.domain.port.out.AuctionClosedListenerPort;
import com.underart.domain.port.out.AuctionRepositoryPort;
import com.underart.domain.port.out.OrderRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BuyNowServiceTest {

    private final AuctionRepositoryPort auctions = mock(AuctionRepositoryPort.class);
    private final OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
    private final ArtworkRepositoryPort artworks = mock(ArtworkRepositoryPort.class);
    private final ActivityLogRepositoryPort log = mock(ActivityLogRepositoryPort.class);
    private final AuctionClosedListenerPort listener = mock(AuctionClosedListenerPort.class);
    private final BuyNowService service = new BuyNowService(auctions, orders, artworks, log, List.of(listener));
    private final UUID artistId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();
    private UUID artworkId;

    @BeforeEach
    void setUp() {
        artworkId = Fixtures.artworkOf(artworks, artistId);
        when(orders.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(auctions.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Auction given(BigDecimal immediatePrice, Instant end) {
        Auction auction = Fixtures.auction(artworkId, AuctionStatus.ACTIVE, immediatePrice, end);
        when(auctions.findByIdForUpdate(auction.id())).thenReturn(Optional.of(auction));
        return auction;
    }

    @Test
    void createsPendingOrderClosesAuctionAndNotifies() {
        Auction auction = given(new BigDecimal("500.00"), Instant.now().plusSeconds(3600));

        Order order = service.buy(auction.id(), buyerId);

        assertThat(order.totalPaid()).isEqualByComparingTo("565.00");
        assertThat(order.platformFee()).isEqualByComparingTo("50.00");
        assertThat(order.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.winnerId()).isEqualTo(buyerId);
        ArgumentCaptor<Auction> saved = ArgumentCaptor.forClass(Auction.class);
        verify(auctions).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(AuctionStatus.CLOSED);
        verify(listener).onAuctionClosed(saved.getValue());
    }

    @Test
    void rejectsEndedAuction() {
        Auction auction = given(new BigDecimal("500"), Instant.now().minusSeconds(60));
        assertThatThrownBy(() -> service.buy(auction.id(), buyerId))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La subasta no está en curso");
        verify(orders, never()).save(any());
    }

    @Test
    void rejectsAuctionWithoutImmediatePrice() {
        Auction auction = given(null, Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> service.buy(auction.id(), buyerId))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Esta subasta no tiene compra inmediata");
    }

    @Test
    void rejectsBuyingOwnArtwork() {
        Auction auction = given(new BigDecimal("500"), Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> service.buy(auction.id(), artistId))
                .isInstanceOf(BusinessRuleException.class).hasMessage("No puedes comprar tu propia obra");
    }
}
