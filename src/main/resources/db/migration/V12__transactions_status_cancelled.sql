ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS transactions_status_check;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_status_check
        CHECK (status IN ('DRAFT', 'PROPOSED', 'APPROVED', 'REJECTED', 'CANCELLED'));
