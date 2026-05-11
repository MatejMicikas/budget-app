-- ========================
-- USERS
-- ========================
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       username      VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(50)  NOT NULL
                           CONSTRAINT users_role_check
                               CHECK (role IN ('ADMIN', 'TREASURER', 'TEAM_LEADER', 'MEMBER'))
);

-- ========================
-- SEASONS
-- ========================
CREATE TABLE seasons (
                         id          BIGSERIAL PRIMARY KEY,
                         name        VARCHAR(100) NOT NULL,
                         date_from   DATE         NOT NULL,
                         date_to     DATE         NOT NULL,
                         status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                             CONSTRAINT seasons_status_check
                                 CHECK (status IN ('OPEN', 'CLOSED')),
                         CONSTRAINT seasons_dates_check
                             CHECK (date_to > date_from)
);

-- ========================
-- FUNDING SOURCES
-- ========================
CREATE TABLE funding_sources (
                                 id               BIGSERIAL PRIMARY KEY,
                                 name             VARCHAR(100) NOT NULL,
                                 type             VARCHAR(50)  NOT NULL
                                     CONSTRAINT funding_sources_type_check
                                         CHECK (type IN ('PUBLIC_GRANT', 'SPONSORSHIP', 'MEMBERSHIP', 'OWN_ACTIVITY')),
                                 allocated_amount NUMERIC(15, 2),
                                 season_id      BIGINT         NOT NULL
                                     REFERENCES seasons (id)
);

-- ========================
-- BUDGET ITEMS
-- ========================
CREATE TABLE budget_items (
                              id                BIGSERIAL PRIMARY KEY,
                              name              VARCHAR(100)   NOT NULL,
                              type              VARCHAR(20)    NOT NULL
                                  CONSTRAINT budget_items_type_check
                                      CHECK (type IN ('INCOME', 'EXPENSE')),
                              planned_amount    NUMERIC(15, 2) NOT NULL
                                  CONSTRAINT budget_items_planned_amount_check
                                      CHECK (planned_amount >= 0),
                              season_id         BIGINT         NOT NULL
                                  REFERENCES seasons (id),
                              funding_source_id BIGINT
                                  REFERENCES funding_sources (id)
);

-- ========================
-- TRANSACTIONS
-- ========================
CREATE TABLE transactions (
                              id             BIGSERIAL PRIMARY KEY,
                              date           DATE           NOT NULL,
                              amount         NUMERIC(15, 2) NOT NULL
                                  CONSTRAINT transactions_amount_check
                                      CHECK (amount > 0),
                              type           VARCHAR(20)    NOT NULL
                                  CONSTRAINT transactions_type_check
                                      CHECK (type IN ('PLANNED', 'ACTUAL')),
                              direction      VARCHAR(20)    NOT NULL
                                  CONSTRAINT transactions_direction_check
                                      CHECK (direction IN ('INCOME', 'EXPENSE')),
                              description    TEXT,
                              budget_item_id BIGINT         NOT NULL
                                  REFERENCES budget_items (id),
                              season_id      BIGINT         NOT NULL
                                  REFERENCES seasons (id)
);

-- ========================
-- AUDIT LOGS
-- ========================
CREATE TABLE audit_logs (
                            id                  BIGSERIAL PRIMARY KEY,
                            timestamp           TIMESTAMP    NOT NULL DEFAULT NOW(),
                            operation_type      VARCHAR(50)  NOT NULL
                                CONSTRAINT audit_logs_operation_type_check
                                    CHECK (operation_type IN (
                                                              'SEASON_CLOSED',
                                                              'TRANSACTION_CREATED',
                                                              'TRANSACTION_DELETED',
                                                              'BUDGET_ITEM_CREATED',
                                                              'BUDGET_ITEM_DELETED',
                                                              'USER_ROLE_CHANGED'
                                        )),
                            affected_entity_id  BIGINT       NOT NULL,
                            performed_by        BIGINT       NOT NULL
                                REFERENCES users (id)
);

-- ========================
-- INDEXY pro výkon
-- ========================
CREATE INDEX idx_budget_items_season_id
    ON budget_items (season_id);

CREATE INDEX idx_transactions_season_id
    ON transactions (season_id);

CREATE INDEX idx_transactions_budget_item_id
    ON transactions (budget_item_id);

CREATE INDEX idx_audit_logs_performed_by
    ON audit_logs (performed_by);

CREATE INDEX idx_audit_logs_timestamp
    ON audit_logs (timestamp);