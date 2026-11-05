CREATE TABLE artwork_media (
    id           uuid                        NOT NULL,
    artwork_id   uuid                        NOT NULL,
    kind         varchar(20)                 NOT NULL,
    url          varchar(1000)               NOT NULL,
    storage_key  varchar(500)                NOT NULL,
    content_type varchar(100)                NOT NULL,
    size_bytes   bigint                      NOT NULL,
    position     integer                     NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT artwork_media_pkey PRIMARY KEY (id),
    CONSTRAINT fk_artwork_media_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id),
    CONSTRAINT artwork_media_kind_check CHECK (kind IN ('IMAGE', 'VIDEO'))
);

CREATE INDEX idx_artwork_media_artwork ON artwork_media (artwork_id, position);
CREATE INDEX idx_artworks_artist ON artworks (artist_id);
CREATE INDEX idx_artwork_tags_artwork ON artwork_tags (artwork_id);
CREATE INDEX idx_auctions_artwork ON auctions (artwork_id);
CREATE INDEX idx_auctions_status_end ON auctions (status, end_date);
CREATE INDEX idx_bids_auction_amount ON bids (auction_id, amount DESC);
CREATE INDEX idx_bids_auction_time ON bids (auction_id, bid_time DESC);
CREATE INDEX idx_bids_buyer ON bids (buyer_id);
CREATE INDEX idx_orders_winner ON orders (winner_id);
CREATE UNIQUE INDEX uq_orders_auction ON orders (auction_id);
