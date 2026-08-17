ALTER TABLE payment_outbox_events
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT,
    ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN dead_lettered BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_payment_outbox_events_pending_retry
    ON payment_outbox_events (published, dead_lettered, next_attempt_at, created_at);
