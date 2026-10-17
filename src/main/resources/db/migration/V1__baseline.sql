CREATE TABLE users (
    id               uuid                        NOT NULL,
    username         varchar(60)                 NOT NULL,
    email            varchar(255)                NOT NULL,
    password_hash    varchar(255)                NOT NULL,
    city             varchar(255),
    shipping_address varchar(255),
    payment_token    varchar(255),
    created_at       timestamp(6) with time zone NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE TABLE artworks (
    id              uuid                        NOT NULL,
    artist_id       uuid                        NOT NULL,
    title           varchar(255)                NOT NULL,
    description     varchar(2000),
    technique       varchar(255),
    weight_kg       numeric(38, 2),
    dimensions      varchar(255),
    suggested_price numeric(38, 2),
    created_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT artworks_pkey PRIMARY KEY (id),
    CONSTRAINT fk_artworks_artist FOREIGN KEY (artist_id) REFERENCES users (id)
);

CREATE TABLE artwork_tags (
    artwork_id uuid NOT NULL,
    tag        varchar(255),
    CONSTRAINT fk_artwork_tags_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id)
);

CREATE TABLE auctions (
    id                       uuid                        NOT NULL,
    artwork_id               uuid                        NOT NULL,
    type                     varchar(20)                 NOT NULL,
    status                   varchar(20)                 NOT NULL,
    base_price               numeric(38, 2)              NOT NULL,
    immediate_purchase_price numeric(38, 2),
    start_date               timestamp(6) with time zone NOT NULL,
    end_date                 timestamp(6) with time zone NOT NULL,
    CONSTRAINT auctions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_auctions_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id),
    CONSTRAINT auctions_status_check CHECK (status IN ('ACTIVE', 'CLOSED', 'CANCELLED')),
    CONSTRAINT auctions_type_check CHECK (type IN ('SIMPLE', 'IMMEDIATE'))
);

CREATE TABLE bids (
    id         uuid                        NOT NULL,
    auction_id uuid                        NOT NULL,
    buyer_id   uuid                        NOT NULL,
    amount     numeric(38, 2)              NOT NULL,
    bid_time   timestamp(6) with time zone NOT NULL,
    CONSTRAINT bids_pkey PRIMARY KEY (id),
    CONSTRAINT fk_bids_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_bids_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
);

CREATE TABLE orders (
    id              uuid                        NOT NULL,
    auction_id      uuid                        NOT NULL,
    winner_id       uuid                        NOT NULL,
    subtotal        numeric(38, 2)              NOT NULL,
    shipping_cost   numeric(38, 2)              NOT NULL,
    platform_fee    numeric(38, 2)              NOT NULL,
    total_paid      numeric(38, 2)              NOT NULL,
    payment_status  varchar(20)                 NOT NULL,
    tracking_number varchar(255),
    created_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT fk_orders_auction FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT fk_orders_winner FOREIGN KEY (winner_id) REFERENCES users (id),
    CONSTRAINT orders_payment_status_check CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED'))
);
