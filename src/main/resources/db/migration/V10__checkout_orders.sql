ALTER TABLE orders ADD COLUMN checkout boolean NOT NULL DEFAULT false;
DROP INDEX uq_orders_auction;
CREATE UNIQUE INDEX uq_orders_auction_commitment ON orders (auction_id)
    WHERE NOT checkout OR payment_status IN ('PROCESSING', 'PAID');
CREATE INDEX idx_checkout_auction ON orders (auction_id) WHERE checkout;
