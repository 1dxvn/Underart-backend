package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Notification;
import com.underart.domain.model.NotificationType;
import com.underart.domain.model.Order;
import com.underart.domain.port.out.NotificationSenderPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionOutcomeNotifier {

    private final NotificationSenderPort notifications;

    public void sold(Artwork artwork, Auction auction, Order order, boolean boughtNow) {
        Map<String, String> data = Map.of("auctionId", auction.id().toString(), "orderId", order.id().toString(),
                "artworkId", artwork.id().toString());
        String price = order.subtotal().toPlainString();
        notifications.send(Notification.create(order.winnerId(), NotificationType.AUCTION_WON,
                boughtNow ? "Compraste una obra" : "¡Ganaste la subasta!",
                (boughtNow ? "Compraste «" : "Ganaste «") + artwork.title() + "» por " + price
                        + ". Paga tu pedido para que el artista pueda enviarlo.", data));
        notifications.send(Notification.create(artwork.artistId(), NotificationType.AUCTION_SOLD,
                "Vendiste tu obra", "«" + artwork.title() + "» se vendió por " + price
                        + ". Te avisaremos cuando el comprador pague.", data));
    }

    public void endedWithoutBids(Artwork artwork, Auction auction) {
        notifications.send(Notification.create(artwork.artistId(), NotificationType.AUCTION_ENDED_NO_BIDS,
                "Tu subasta terminó sin pujas",
                "«" + artwork.title() + "» no recibió pujas. Puedes crear otra subasta.",
                Map.of("auctionId", auction.id().toString(), "artworkId", artwork.id().toString())));
    }
}
