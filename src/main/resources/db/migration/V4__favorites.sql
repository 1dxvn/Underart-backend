CREATE TABLE favorites (
    user_id    uuid                        NOT NULL,
    artwork_id uuid                        NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT favorites_pkey PRIMARY KEY (user_id, artwork_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_artwork FOREIGN KEY (artwork_id) REFERENCES artworks (id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_artwork ON favorites (artwork_id);
CREATE INDEX idx_favorites_user_created ON favorites (user_id, created_at DESC);
