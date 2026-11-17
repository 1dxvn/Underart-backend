CREATE UNIQUE INDEX uq_auctions_active_artwork ON auctions (artwork_id) WHERE status = 'ACTIVE';
ALTER TABLE orders DROP CONSTRAINT orders_payment_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check
    CHECK (payment_status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'EXPIRED'));
ALTER TABLE orders ADD COLUMN payment_due_at timestamp with time zone;
ALTER TABLE orders ADD COLUMN snapshot jsonb;
ALTER TABLE orders ADD COLUMN payment_reference varchar(255);
ALTER TABLE orders ADD COLUMN payment_method varchar(255);
ALTER TABLE orders ADD COLUMN transaction_id varchar(255);
ALTER TABLE orders ADD COLUMN payment_started_at timestamp with time zone;
UPDATE orders SET payment_due_at = created_at + interval '48 hours';
UPDATE orders o SET snapshot = jsonb_build_object(
    'title', a.title, 'description', a.description, 'technique', a.technique,
    'dimensions', a.dimensions, 'currency', 'usd', 'recipient', u.username,
    'shippingAddress', u.shipping_address, 'city', u.city)
FROM auctions s JOIN artworks a ON a.id = s.artwork_id, users u
WHERE o.auction_id = s.id AND o.winner_id = u.id;
ALTER TABLE orders ALTER COLUMN payment_due_at SET NOT NULL;
CREATE INDEX idx_orders_unpaid_deadline ON orders (payment_due_at)
    WHERE payment_status IN ('PENDING', 'FAILED');
