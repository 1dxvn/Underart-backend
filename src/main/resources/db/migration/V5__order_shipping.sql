ALTER TABLE orders ADD COLUMN shipping_status varchar(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE orders ADD COLUMN carrier varchar(100);
ALTER TABLE orders ADD COLUMN shipped_at timestamp(6) with time zone;
ALTER TABLE orders ADD COLUMN delivered_at timestamp(6) with time zone;
ALTER TABLE orders ADD CONSTRAINT orders_shipping_status_check
    CHECK (shipping_status IN ('PENDING', 'SHIPPED', 'DELIVERED'));

-- Antes se generaba un número de guía aleatorio al crear la orden; ahora lo informa el vendedor al enviar.
UPDATE orders SET tracking_number = NULL WHERE shipping_status = 'PENDING';
