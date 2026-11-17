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
                boughtNow ? "You bought an artwork" : "You won the auction!",
                (boughtNow ? "You bought «" : "You won «") + artwork.title() + "» for " + price
                        + ". Pay your order so the artist can ship it.", data));
        notifications.send(Notification.create(artwork.artistId(), NotificationType.AUCTION_SOLD,
                "Your artwork sold", "«" + artwork.title() + "» sold for " + price
                        + ". We will notify you when the buyer pays.", data));
    }

    public void endedWithoutBids(Artwork artwork, Auction auction) {
        notifications.send(Notification.create(artwork.artistId(), NotificationType.AUCTION_ENDED_NO_BIDS,
                "Your auction ended without bids",
                "«" + artwork.title() + "» received no bids. You can create another auction.",
                Map.of("auctionId", auction.id().toString(), "artworkId", artwork.id().toString())));
    }
}
