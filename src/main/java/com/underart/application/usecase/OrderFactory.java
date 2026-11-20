package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Auction;
import com.underart.domain.model.Order;
import com.underart.domain.model.PurchaseSnapshot;
import com.underart.domain.model.ShippingEstimate;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderFactory {

    private final String currency;

    public OrderFactory(@Value("${payment.currency:usd}") String currency) {
        if (!currency.matches("[a-zA-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter code");
        }
        this.currency = currency.toLowerCase(Locale.ROOT);
    }

    public Order create(Auction auction, UUID buyerId, BigDecimal subtotal, Artwork artwork) {
        String saleCurrency = auction.currency() == null ? currency : auction.currency().name().toLowerCase(Locale.ROOT);
        return Order.pending(auction.id(), buyerId, subtotal, ShippingEstimate.forWeight(saleCurrency, artwork.weightKg())).withSnapshot(PurchaseSnapshot.of(artwork, saleCurrency));
    }
}
