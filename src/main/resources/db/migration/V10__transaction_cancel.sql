ALTER TABLE transactions
    ADD COLUMN cancelled_at TIMESTAMP,
    ADD COLUMN cancel_reason VARCHAR(2000),
    ADD COLUMN cancelled_by BIGINT REFERENCES users (id) ON DELETE RESTRICT;

CREATE INDEX idx_transactions_cancelled_by ON transactions (cancelled_by);
