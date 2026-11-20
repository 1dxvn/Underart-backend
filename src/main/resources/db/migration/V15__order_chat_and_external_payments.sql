ALTER TABLE orders DROP CONSTRAINT orders_payment_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check
    CHECK (payment_status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'EXPIRED', 'COORDINATING', 'REPORTED'));
DROP INDEX uq_orders_auction_commitment;
CREATE UNIQUE INDEX uq_orders_auction_commitment ON orders (auction_id)
    WHERE NOT checkout OR payment_status IN ('PROCESSING', 'PAID', 'COORDINATING', 'REPORTED');
CREATE INDEX idx_external_coordination_expiry ON orders (payment_due_at) WHERE payment_status = 'COORDINATING';

CREATE TABLE order_messages (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sender_id uuid NOT NULL REFERENCES users(id),
    message varchar(2000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_messages_order ON order_messages (order_id, id);
