CREATE TABLE activity_events (
    id text PRIMARY KEY,
    event_type text NOT NULL,
    description text,
    user_id uuid,
    metadata jsonb,
    created_at timestamptz NOT NULL
);

CREATE INDEX activity_events_by_user ON activity_events (user_id, created_at DESC, id DESC);

CREATE TABLE user_notifications (
    id text PRIMARY KEY,
    user_id uuid NOT NULL,
    notification_type text NOT NULL,
    title text NOT NULL,
    message text NOT NULL,
    data jsonb NOT NULL,
    is_read boolean NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX user_notifications_by_user ON user_notifications (user_id, created_at DESC, id DESC);
CREATE INDEX unread_notifications_by_user ON user_notifications (user_id, created_at DESC, id DESC)
    WHERE NOT is_read;

CREATE TABLE ai_generations (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider text NOT NULL,
    artwork_title text NOT NULL,
    suggested_price numeric,
    tags jsonb NOT NULL,
    raw_response text,
    created_at timestamptz NOT NULL
);
