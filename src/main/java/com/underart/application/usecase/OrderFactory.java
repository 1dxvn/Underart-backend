package com.underart.application.usecase;

import com.underart.domain.model.Artwork;
import com.underart.domain.model.Order;
import com.underart.domain.model.PurchaseSnapshot;
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

    public Order create(UUID auctionId, UUID buyerId, BigDecimal subtotal, Artwork artwork) {
        return Order.pending(auctionId, buyerId, subtotal).withSnapshot(PurchaseSnapshot.of(artwork, currency));
    }
}
