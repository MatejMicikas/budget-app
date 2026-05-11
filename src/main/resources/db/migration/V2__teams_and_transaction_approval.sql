CREATE TABLE teams (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

ALTER TABLE users
    ADD COLUMN team_id BIGINT REFERENCES teams (id);

ALTER TABLE budget_items
    ADD COLUMN team_id BIGINT REFERENCES teams (id);

ALTER TABLE transactions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT transactions_status_check CHECK (status IN ('DRAFT', 'PROPOSED', 'APPROVED', 'REJECTED')),
    ADD COLUMN proposed_by BIGINT REFERENCES users (id),
    ADD COLUMN approved_by BIGINT REFERENCES users (id),
    ADD COLUMN approved_at TIMESTAMP;

UPDATE transactions
SET status = CASE
    WHEN type = 'PLANNED' THEN 'APPROVED'
    ELSE 'APPROVED'
END;

CREATE INDEX idx_users_team_id ON users (team_id);
CREATE INDEX idx_budget_items_team_id ON budget_items (team_id);
CREATE INDEX idx_transactions_status ON transactions (status);
