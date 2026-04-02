-- Phase 5 optional modules: IMS pending-authorization segments (flattened) + DB2 AUTHFRDS (fraud).

CREATE TABLE pending_auth_summary (
    acct_id              BIGINT       PRIMARY KEY REFERENCES accounts (acct_id),
    cust_id              BIGINT       NOT NULL,
    auth_status          VARCHAR(1),
    acct_status_1        VARCHAR(2),
    acct_status_2        VARCHAR(2),
    acct_status_3        VARCHAR(2),
    acct_status_4        VARCHAR(2),
    acct_status_5        VARCHAR(2),
    credit_limit         NUMERIC(12,2),
    cash_limit           NUMERIC(12,2),
    credit_balance       NUMERIC(12,2),
    cash_balance         NUMERIC(12,2),
    approved_auth_count  INTEGER,
    declined_auth_count  INTEGER,
    approved_auth_amt    NUMERIC(12,2),
    declined_auth_amt    NUMERIC(12,2)
);

CREATE TABLE pending_auth_detail (
    id                   BIGSERIAL    PRIMARY KEY,
    acct_id              BIGINT       NOT NULL REFERENCES pending_auth_summary (acct_id),
    auth_ts              TIMESTAMP    NOT NULL,
    card_num             VARCHAR(16)  NOT NULL,
    auth_type            VARCHAR(4),
    card_expiry_date     VARCHAR(4),
    message_type         VARCHAR(6),
    message_source       VARCHAR(6),
    auth_id_code         VARCHAR(6),
    auth_resp_code       VARCHAR(2),
    auth_resp_reason     VARCHAR(4),
    processing_code      VARCHAR(6),
    transaction_amt      NUMERIC(12,2),
    approved_amt         NUMERIC(12,2),
    merchant_category_cd VARCHAR(4),
    acqr_country_code    VARCHAR(3),
    pos_entry_mode       INTEGER,
    merchant_id          VARCHAR(15),
    merchant_name        VARCHAR(22),
    merchant_city        VARCHAR(13),
    merchant_state       VARCHAR(2),
    merchant_zip         VARCHAR(9),
    transaction_id       VARCHAR(15)  NOT NULL,
    match_status         VARCHAR(1),
    fraud_flag           VARCHAR(1),
    fraud_report_date    VARCHAR(8),
    UNIQUE (acct_id, transaction_id)
);

CREATE INDEX idx_pending_auth_detail_acct ON pending_auth_detail (acct_id);
CREATE INDEX idx_pending_auth_detail_match ON pending_auth_detail (match_status);

CREATE TABLE auth_fraud_reports (
    card_num             VARCHAR(16)  NOT NULL,
    auth_ts              TIMESTAMP    NOT NULL,
    auth_type            VARCHAR(4),
    card_expiry_date     VARCHAR(4),
    message_type         VARCHAR(6),
    message_source       VARCHAR(6),
    auth_id_code         VARCHAR(6),
    auth_resp_code       VARCHAR(2),
    auth_resp_reason     VARCHAR(4),
    processing_code      VARCHAR(6),
    transaction_amt      NUMERIC(12,2),
    approved_amt         NUMERIC(12,2),
    merchant_catagory_cd VARCHAR(4),
    acqr_country_code    VARCHAR(3),
    pos_entry_mode       SMALLINT,
    merchant_id          VARCHAR(15),
    merchant_name        VARCHAR(22),
    merchant_city        VARCHAR(13),
    merchant_state       VARCHAR(2),
    merchant_zip         VARCHAR(9),
    transaction_id       VARCHAR(15)  NOT NULL,
    match_status         VARCHAR(1),
    auth_fraud           VARCHAR(1),
    fraud_rpt_date       DATE,
    acct_id              BIGINT,
    cust_id              BIGINT,
    PRIMARY KEY (card_num, auth_ts)
);
