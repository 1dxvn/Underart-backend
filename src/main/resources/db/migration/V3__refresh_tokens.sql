CREATE TABLE refresh_tokens (
    id          uuid                        NOT NULL,
    user_id     uuid                        NOT NULL,
    token_hash  varchar(64)                 NOT NULL,
    family_id   uuid                        NOT NULL,
    expires_at  timestamp(6) with time zone NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL,
    revoked_at  timestamp(6) with time zone,
    replaced_by uuid,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
