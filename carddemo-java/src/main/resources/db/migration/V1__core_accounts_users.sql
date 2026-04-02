-- Minimal core schema for Phase 5 optional modules (accounts FK, security users).

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    user_type   VARCHAR(1)   NOT NULL DEFAULT 'U'
);

CREATE TABLE accounts (
    acct_id                 BIGINT        PRIMARY KEY,
    active_status           VARCHAR(1)    NOT NULL DEFAULT 'A',
    current_balance         NUMERIC(12,2) NOT NULL DEFAULT 0,
    credit_limit            NUMERIC(12,2) NOT NULL DEFAULT 0,
    cash_credit_limit       NUMERIC(12,2) NOT NULL DEFAULT 0,
    open_date               VARCHAR(10),
    expiration_date         VARCHAR(10),
    reissue_date            VARCHAR(10),
    current_cycle_credit    NUMERIC(12,2) NOT NULL DEFAULT 0,
    current_cycle_debit     NUMERIC(12,2) NOT NULL DEFAULT 0,
    addr_zip                VARCHAR(10),
    group_id                VARCHAR(10)
);
