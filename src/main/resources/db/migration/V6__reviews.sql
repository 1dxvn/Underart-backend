CREATE TABLE reviews (
    id          uuid                        NOT NULL,
    order_id    uuid                        NOT NULL,
    reviewer_id uuid                        NOT NULL,
    seller_id   uuid                        NOT NULL,
    rating      integer                     NOT NULL,
    comment     varchar(1000),
    created_at  timestamp(6) with time zone NOT NULL,
    CONSTRAINT reviews_pkey PRIMARY KEY (id),
    CONSTRAINT uq_reviews_order UNIQUE (order_id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT reviews_rating_check CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_seller_created ON reviews (seller_id, created_at DESC);
