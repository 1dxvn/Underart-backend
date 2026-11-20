UPDATE orders o
SET shipping_cost = 15000 + 3000 * greatest(ceil(coalesce(w.weight_kg, 1)) - 1, 0),
    total_paid = o.subtotal + o.platform_fee + 15000 + 3000 * greatest(ceil(coalesce(w.weight_kg, 1)) - 1, 0)
FROM auctions a JOIN artworks w ON w.id = a.artwork_id
WHERE o.auction_id = a.id
  AND o.checkout AND o.payment_status = 'PENDING' AND o.payment_started_at IS NULL
  AND lower(o.snapshot ->> 'currency') = 'cop' AND o.shipping_cost = 15;
