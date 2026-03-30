-- CardDemo schema: VSAM file layouts mapped to PostgreSQL tables.
-- Each table corresponds to a COBOL copybook record layout and its VSAM KSDS file.

CREATE TABLE accounts (
    acct_id           BIGINT       PRIMARY KEY,  -- PIC 9(11)
    active_status     VARCHAR(1),                 -- PIC X(01)
    curr_bal          NUMERIC(12,2),              -- PIC S9(10)V99
    credit_limit      NUMERIC(12,2),              -- PIC S9(10)V99
    cash_credit_limit NUMERIC(12,2),              -- PIC S9(10)V99
    open_date         VARCHAR(10),                -- PIC X(10)
    expiration_date   VARCHAR(10),                -- PIC X(10)
    reissue_date      VARCHAR(10),                -- PIC X(10)
    curr_cyc_credit   NUMERIC(12,2),              -- PIC S9(10)V99
    curr_cyc_debit    NUMERIC(12,2),              -- PIC S9(10)V99
    addr_zip          VARCHAR(10),                -- PIC X(10)
    group_id          VARCHAR(10)                 -- PIC X(10)
);

CREATE TABLE customers (
    cust_id              BIGINT       PRIMARY KEY,  -- PIC 9(09)
    first_name           VARCHAR(25),               -- PIC X(25)
    middle_name          VARCHAR(25),               -- PIC X(25)
    last_name            VARCHAR(25),               -- PIC X(25)
    addr_line_1          VARCHAR(50),               -- PIC X(50)
    addr_line_2          VARCHAR(50),               -- PIC X(50)
    addr_line_3          VARCHAR(50),               -- PIC X(50)
    addr_state_cd        VARCHAR(2),                -- PIC X(02)
    addr_country_cd      VARCHAR(3),                -- PIC X(03)
    addr_zip             VARCHAR(10),               -- PIC X(10)
    phone_num_1          VARCHAR(15),               -- PIC X(15)
    phone_num_2          VARCHAR(15),               -- PIC X(15)
    ssn                  BIGINT,                    -- PIC 9(09)
    govt_issued_id       VARCHAR(20),               -- PIC X(20)
    dob                  VARCHAR(10),               -- PIC X(10)
    eft_account_id       VARCHAR(10),               -- PIC X(10)
    pri_card_holder_ind  VARCHAR(1),                -- PIC X(01)
    fico_credit_score    INTEGER                    -- PIC 9(03)
);

CREATE TABLE cards (
    card_num         VARCHAR(16)  PRIMARY KEY,  -- PIC X(16)
    acct_id          BIGINT,                    -- PIC 9(11)
    cvv_cd           INTEGER,                   -- PIC 9(03)
    embossed_name    VARCHAR(50),               -- PIC X(50)
    expiration_date  VARCHAR(10),               -- PIC X(10)
    active_status    VARCHAR(1)                 -- PIC X(01)
);

CREATE TABLE card_xrefs (
    card_num  VARCHAR(16)  PRIMARY KEY,  -- PIC X(16)
    cust_id   BIGINT,                    -- PIC 9(09)
    acct_id   BIGINT                     -- PIC 9(11)
);

CREATE TABLE transactions (
    tran_id        VARCHAR(16)  PRIMARY KEY,  -- PIC X(16)
    type_cd        VARCHAR(2),                -- PIC X(02)
    cat_cd         INTEGER,                   -- PIC 9(04)
    source         VARCHAR(10),               -- PIC X(10)
    description    VARCHAR(100),              -- PIC X(100)
    amount         NUMERIC(11,2),             -- PIC S9(09)V99
    merchant_id    BIGINT,                    -- PIC 9(09)
    merchant_name  VARCHAR(50),               -- PIC X(50)
    merchant_city  VARCHAR(50),               -- PIC X(50)
    merchant_zip   VARCHAR(10),               -- PIC X(10)
    card_num       VARCHAR(16),               -- PIC X(16)
    orig_ts        VARCHAR(26),               -- PIC X(26)
    proc_ts        VARCHAR(26)                -- PIC X(26)
);

CREATE TABLE transaction_category_balances (
    acct_id   BIGINT      NOT NULL,  -- PIC 9(11)
    type_cd   VARCHAR(2)  NOT NULL,  -- PIC X(02)
    cat_cd    INTEGER     NOT NULL,  -- PIC 9(04)
    balance   NUMERIC(11,2),         -- PIC S9(09)V99
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);

CREATE TABLE users (
    user_id    VARCHAR(8)   PRIMARY KEY,  -- PIC X(08)
    first_name VARCHAR(20),               -- PIC X(20)
    last_name  VARCHAR(20),               -- PIC X(20)
    password   VARCHAR(60),               -- PIC X(08) stored as BCrypt hash (60 chars)
    user_type  VARCHAR(1)                 -- PIC X(01): 'A' = admin, 'U' = regular
);

CREATE TABLE disclosure_groups (
    acct_group_id  VARCHAR(10)  NOT NULL,  -- PIC X(10)
    tran_type_cd   VARCHAR(2)   NOT NULL,  -- PIC X(02)
    tran_cat_cd    INTEGER      NOT NULL,  -- PIC 9(04)
    interest_rate  NUMERIC(6,2),           -- PIC S9(04)V99
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd)
);

CREATE TABLE transaction_types (
    type_cd    VARCHAR(2)   PRIMARY KEY,  -- PIC X(02)
    type_desc  VARCHAR(50)               -- PIC X(50)
);

CREATE TABLE transaction_categories (
    type_cd      VARCHAR(2)  NOT NULL,  -- PIC X(02)
    cat_cd       INTEGER     NOT NULL,  -- PIC 9(04)
    description  VARCHAR(50),           -- PIC X(50)
    PRIMARY KEY (type_cd, cat_cd)
);

-- Indexes matching VSAM alternate index paths
CREATE INDEX idx_cards_acct_id ON cards (acct_id);
CREATE INDEX idx_card_xrefs_acct_id ON card_xrefs (acct_id);
CREATE INDEX idx_card_xrefs_cust_id ON card_xrefs (cust_id);
CREATE INDEX idx_transactions_card_num ON transactions (card_num);
CREATE INDEX idx_tcat_bal_acct_id ON transaction_category_balances (acct_id);
