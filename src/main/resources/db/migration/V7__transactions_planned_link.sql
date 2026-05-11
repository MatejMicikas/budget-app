ALTER TABLE transactions
    ADD COLUMN planned_transaction_id BIGINT REFERENCES transactions (id) ON DELETE SET NULL;

CREATE INDEX idx_transactions_planned_transaction_id
    ON transactions (planned_transaction_id);
