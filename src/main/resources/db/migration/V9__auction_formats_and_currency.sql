ALTER TABLE auctions DROP CONSTRAINT auctions_type_check;
ALTER TABLE auctions ADD CONSTRAINT auctions_type_check CHECK (type IN ('SIMPLE', 'IMMEDIATE', 'DUTCH', 'SEALED'));
ALTER TABLE auctions ADD COLUMN currency varchar(3) CHECK (currency IN ('USD', 'COP', 'EUR'));
ALTER TABLE auctions ADD COLUMN floor_price numeric(38, 2);
ALTER TABLE auctions ADD CONSTRAINT auctions_floor_price_check CHECK (
    (type = 'DUTCH' AND floor_price IS NOT NULL AND floor_price > 0 AND floor_price < base_price)
    OR (type <> 'DUTCH' AND floor_price IS NULL)
);
