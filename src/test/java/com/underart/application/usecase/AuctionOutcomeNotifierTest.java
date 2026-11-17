package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.AuctionStatus;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.port.out.NotificationSenderPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuctionOutcomeNotifierTest {

    private final NotificationSenderPort sender = mock(NotificationSenderPort.class);
    private final AuctionOutcomeNotifier notifier = new AuctionOutcomeNotifier(sender);
    private final Artwork artwork = new Artwork(UUID.randomUUID(), UUID.randomUUID(), "Marina", null, null, null,
            null, null, List.of(), Instant.now());
    private final Auction auction = Fixtures.auction(artwork.id(), AuctionStatus.CLOSED, null, Instant.now());

    @Test
    void soldNotifiesWinnerAndSellerWithTheOrder() {
        Order order = Order.pending(auction.id(), UUID.randomUUID(), new BigDecimal("250.00"));

        notifier.sold(artwork, auction, order, false);

        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(sender, times(2)).send(sent.capture());
        Notification toWinner = sent.getAllValues().get(0);
        Notification toSeller = sent.getAllValues().get(1);
        assertThat(toWinner.userId()).isEqualTo(order.winnerId());
        assertThat(toWinner.type()).isEqualTo(NotificationType.AUCTION_WON);
        assertThat(toWinner.message()).contains("Marina", "250.00");
        assertThat(toWinner.data()).containsEntry("orderId", order.id().toString());
        assertThat(toSeller.userId()).isEqualTo(artwork.artistId());
        assertThat(toSeller.type()).isEqualTo(NotificationType.AUCTION_SOLD);
    }

    @Test
    void endedWithoutBidsNotifiesOnlyTheSeller() {
        notifier.endedWithoutBids(artwork, auction);

        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(sender).send(sent.capture());
        assertThat(sent.getValue().userId()).isEqualTo(artwork.artistId());
        assertThat(sent.getValue().type()).isEqualTo(NotificationType.AUCTION_ENDED_NO_BIDS);
    }
}
