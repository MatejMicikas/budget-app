-- Ensure season names are unique.
ALTER TABLE seasons
    ADD CONSTRAINT seasons_name_unique UNIQUE (name);

-- Composite uniqueness needed for composite FK (transaction -> budget_item + season consistency).
ALTER TABLE budget_items
    ADD CONSTRAINT budget_items_id_season_unique UNIQUE (id, season_id);

-- Rebuild foreign keys with explicit ON DELETE strategies.
ALTER TABLE funding_sources
    DROP CONSTRAINT IF EXISTS funding_sources_season_id_fkey,
    ADD CONSTRAINT funding_sources_season_id_fkey
        FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE RESTRICT;

ALTER TABLE budget_items
    DROP CONSTRAINT IF EXISTS budget_items_season_id_fkey,
    ADD CONSTRAINT budget_items_season_id_fkey
        FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE RESTRICT,
    DROP CONSTRAINT IF EXISTS budget_items_funding_source_id_fkey,
    ADD CONSTRAINT budget_items_funding_source_id_fkey
        FOREIGN KEY (funding_source_id) REFERENCES funding_sources (id) ON DELETE SET NULL,
    DROP CONSTRAINT IF EXISTS budget_items_team_id_fkey,
    ADD CONSTRAINT budget_items_team_id_fkey
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_team_id_fkey,
    ADD CONSTRAINT users_team_id_fkey
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL;

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS transactions_budget_item_id_fkey,
    DROP CONSTRAINT IF EXISTS transactions_season_id_fkey,
    ADD CONSTRAINT transactions_budget_item_season_fkey
        FOREIGN KEY (budget_item_id, season_id) REFERENCES budget_items (id, season_id) ON DELETE RESTRICT,
    ADD CONSTRAINT transactions_season_id_fkey
        FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE RESTRICT,
    DROP CONSTRAINT IF EXISTS transactions_proposed_by_fkey,
    ADD CONSTRAINT transactions_proposed_by_fkey
        FOREIGN KEY (proposed_by) REFERENCES users (id) ON DELETE SET NULL,
    DROP CONSTRAINT IF EXISTS transactions_approved_by_fkey,
    ADD CONSTRAINT transactions_approved_by_fkey
        FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_performed_by_fkey,
    ADD CONSTRAINT audit_logs_performed_by_fkey
        FOREIGN KEY (performed_by) REFERENCES users (id) ON DELETE RESTRICT;
