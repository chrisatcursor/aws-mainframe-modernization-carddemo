-- Phase 5: pending authorization (IMS segments), fraud (AUTHFRDS), transaction category code (DB2 DCLTRCAT alignment)

CREATE TABLE fraud_reports (
    card_num              CHAR(16)        NOT NULL,
    auth_ts               TIMESTAMP       NOT NULL,
    auth_type             CHAR(4),
    card_expiry_date      CHAR(4),
    message_type          CHAR(6),
    message_source        CHAR(6),
    auth_id_code          CHAR(6),
    auth_resp_code        CHAR(2),
    auth_resp_reason      CHAR(4),
    processing_code       CHAR(6),
    transaction_amt       NUMERIC(12, 2),
    approved_amt          NUMERIC(12, 2),
    merchant_category_code CHAR(4),
    acqr_country_code     CHAR(3),
    pos_entry_mode        SMALLINT,
    merchant_id           CHAR(15),
    merchant_name         VARCHAR(22),
    merchant_city         CHAR(13),
    merchant_state        CHAR(2),
    merchant_zip          CHAR(9),
    transaction_id        CHAR(15),
    match_status          CHAR(1),
    auth_fraud            CHAR(1),
    fraud_rpt_date        DATE,
    acct_id               NUMERIC(11, 0),
    cust_id               NUMERIC(9, 0),
    PRIMARY KEY (card_num, auth_ts)
);

CREATE TABLE pending_auth_summaries (
    acct_id                 BIGINT          NOT NULL PRIMARY KEY,
    cust_id                 BIGINT          NOT NULL,
    auth_status             CHAR(1),
    account_status_1        CHAR(2),
    account_status_2        CHAR(2),
    account_status_3        CHAR(2),
    account_status_4        CHAR(2),
    account_status_5        CHAR(2),
    credit_limit            NUMERIC(12, 2),
    cash_limit              NUMERIC(12, 2),
    credit_balance          NUMERIC(12, 2),
    cash_balance            NUMERIC(12, 2),
    approved_auth_cnt       INTEGER         NOT NULL DEFAULT 0,
    declined_auth_cnt       INTEGER         NOT NULL DEFAULT 0,
    approved_auth_amt      NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    declined_auth_amt      NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_pas_account FOREIGN KEY (acct_id) REFERENCES accounts (acct_id)
);

CREATE TABLE pending_auth_details (
    id                      BIGSERIAL       PRIMARY KEY,
    acct_id                 BIGINT          NOT NULL,
    auth_date_9c            INTEGER         NOT NULL,
    auth_time_9c            INTEGER         NOT NULL,
    auth_orig_date          CHAR(6),
    auth_orig_time          CHAR(6),
    card_num                CHAR(16)        NOT NULL,
    auth_type               CHAR(4),
    card_expiry_date        CHAR(4),
    message_type            CHAR(6),
    message_source          CHAR(6),
    auth_id_code            CHAR(6),
    auth_resp_code          CHAR(2),
    auth_resp_reason        CHAR(4),
    processing_code         VARCHAR(6),
    transaction_amt         NUMERIC(12, 2),
    approved_amt            NUMERIC(12, 2),
    merchant_category_code  CHAR(4),
    acqr_country_code       CHAR(3),
    pos_entry_mode          SMALLINT,
    merchant_id             CHAR(15),
    merchant_name           VARCHAR(22),
    merchant_city           CHAR(13),
    merchant_state          CHAR(2),
    merchant_zip            CHAR(9),
    transaction_id          CHAR(15),
    match_status            CHAR(1),
    auth_fraud              CHAR(1),
    fraud_rpt_date          CHAR(8),
    CONSTRAINT fk_pad_summary FOREIGN KEY (acct_id) REFERENCES pending_auth_summaries (acct_id)
);

CREATE UNIQUE INDEX ux_pending_auth_detail_natural ON pending_auth_details (acct_id, auth_date_9c, auth_time_9c, card_num);
CREATE INDEX idx_pending_auth_details_acct ON pending_auth_details (acct_id);

ALTER TABLE transaction_categories
    ADD COLUMN IF NOT EXISTS category_code CHAR(4);

UPDATE transaction_categories
SET category_code = LPAD(cat_cd::TEXT, 4, '0')
WHERE category_code IS NULL;

ALTER TABLE transaction_categories
    ALTER COLUMN category_code SET NOT NULL;

CREATE UNIQUE INDEX ux_transaction_categories_type_catcode ON transaction_categories (type_cd, category_code);
