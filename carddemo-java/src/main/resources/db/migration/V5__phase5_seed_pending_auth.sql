-- Demo pending authorization for account 1 (same calendar day as DB migration run → not expired for default 5-day purge)
INSERT INTO pending_auth_summaries (
    acct_id, cust_id, auth_status,
    account_status_1, account_status_2, account_status_3, account_status_4, account_status_5,
    credit_limit, cash_limit, credit_balance, cash_balance,
    approved_auth_cnt, declined_auth_cnt, approved_auth_amt, declined_auth_amt, version
) VALUES (
    1, 1, 'A',
    'Y', 'Y', 'Y', 'Y', 'Y',
    2020.00, 1020.00, 194.00, 0.00,
    1, 0, 50.00, 0.00, 0
);

INSERT INTO pending_auth_details (
    acct_id, auth_date_9c, auth_time_9c, auth_orig_date, auth_orig_time,
    card_num, auth_type, card_expiry_date, message_type, message_source,
    auth_id_code, auth_resp_code, auth_resp_reason, processing_code,
    transaction_amt, approved_amt, merchant_category_code, acqr_country_code, pos_entry_mode,
    merchant_id, merchant_name, merchant_city, merchant_state, merchant_zip,
    transaction_id, match_status, auth_fraud, fraud_rpt_date
)
SELECT
    1,
    99999 - (((EXTRACT(YEAR FROM CURRENT_DATE)::INT % 100) * 1000) + EXTRACT(DOY FROM CURRENT_DATE)::INT),
    856977999,
    TO_CHAR(CURRENT_DATE, 'YYMMDD'),
    TO_CHAR(CURRENT_TIMESTAMP, 'HH24MISS'),
    '9680294154603697', 'AUTH', '1225', 'AUTHRQ', 'POS',
    'AUTH01', '00', '0000', '000001',
    50.00, 50.00, '5411', '840', 1,
    'MERCH0000000001', 'Demo Merchant', 'Seattle', 'WA', '98101',
    'TXN-DEMO-00001', 'P', ' ', '        '
WHERE NOT EXISTS (SELECT 1 FROM pending_auth_details WHERE acct_id = 1);
