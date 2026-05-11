ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_operation_type_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_operation_type_check
        CHECK (operation_type IN (
            'SEASON_CLOSED',
            'SEASON_CREATED',
            'SEASON_UPDATED',
            'TRANSACTION_CREATED',
            'TRANSACTION_UPDATED',
            'TRANSACTION_APPROVED',
            'TRANSACTION_REJECTED',
            'TRANSACTION_DELETED',
            'BUDGET_ITEM_CREATED',
            'BUDGET_ITEM_UPDATED',
            'BUDGET_ITEM_DELETED',
            'USER_ROLE_CHANGED',
            'FUNDING_SOURCE_CREATED',
            'FUNDING_SOURCE_UPDATED',
            'FUNDING_SOURCE_DELETED'
        ));
