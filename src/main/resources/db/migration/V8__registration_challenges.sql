CREATE TABLE registration_challenges (
    email varchar(255) PRIMARY KEY,
    code_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    resend_at timestamptz NOT NULL,
    attempts integer NOT NULL CHECK (attempts BETWEEN 0 AND 5)
);

CREATE INDEX registration_challenges_expires_idx ON registration_challenges (expires_at);
