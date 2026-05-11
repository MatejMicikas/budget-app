-- Deleting a team must not silently clear team_id on users/budget_items (SET NULL breaks TEAM_LEADER assignment and reporting).
ALTER TABLE budget_items
    DROP CONSTRAINT IF EXISTS budget_items_team_id_fkey,
    ADD CONSTRAINT budget_items_team_id_fkey
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_team_id_fkey,
    ADD CONSTRAINT users_team_id_fkey
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT;
