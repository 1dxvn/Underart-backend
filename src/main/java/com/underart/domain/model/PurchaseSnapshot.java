package com.underart.domain.model;

public record PurchaseSnapshot(String title, String description, String technique, String dimensions,
                               String currency, String recipient, String shippingAddress, String city) {

    public static PurchaseSnapshot of(Artwork artwork, String currency) {
        return new PurchaseSnapshot(artwork.title(), artwork.description(), artwork.technique(),
                artwork.dimensions(), currency, null, null, null);
    }

    public PurchaseSnapshot withDelivery(User buyer) {
        return new PurchaseSnapshot(title, description, technique, dimensions, currency,
                buyer.username(), buyer.shippingAddress(), buyer.city());
    }
}
