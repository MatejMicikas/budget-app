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
            'BUDGET_ITEM_TEAM_ASSIGNED',
            'BUDGET_ITEM_TEAM_UNASSIGNED',
            'USER_ROLE_CHANGED',
            'USER_TEAM_ASSIGNED',
            'USER_TEAM_UNASSIGNED',
            'FUNDING_SOURCE_CREATED',
            'FUNDING_SOURCE_UPDATED',
            'FUNDING_SOURCE_DELETED',
            'TEAM_CREATED',
            'TEAM_UPDATED',
            'TEAM_DELETED'
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
            'FUNDING_SOURCE',
            'TEAM'
        ));
