ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS affected_entity_type VARCHAR(50);

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS old_value_json TEXT,
    ADD COLUMN IF NOT EXISTS new_value_json TEXT;

UPDATE audit_logs
SET affected_entity_type = COALESCE(affected_entity_type, 'USER');

ALTER TABLE audit_logs
    ALTER COLUMN affected_entity_type SET NOT NULL;

ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_operation_type_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_operation_type_check
        CHECK (operation_type IN (
            'SEASON_CLOSED',
            'SEASON_CREATED',
            'TRANSACTION_CREATED',
            'TRANSACTION_UPDATED',
            'TRANSACTION_APPROVED',
            'TRANSACTION_DELETED',
            'BUDGET_ITEM_CREATED',
            'BUDGET_ITEM_UPDATED',
            'BUDGET_ITEM_DELETED',
            'USER_ROLE_CHANGED',
            'FUNDING_SOURCE_CREATED',
            'FUNDING_SOURCE_UPDATED',
            'FUNDING_SOURCE_DELETED'
        ));

ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_entity_type_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_entity_type_check
        CHECK (affected_entity_type IN (
            'SEASON',
            'TRANSACTION',
            'BUDGET_ITEM',
            'USER',
            'FUNDING_SOURCE'
        ));
