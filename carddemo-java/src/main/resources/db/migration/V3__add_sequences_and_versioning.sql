-- Phase 4: Add optimistic locking version columns and transaction ID sequence.

ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cards ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE SEQUENCE transaction_id_seq START WITH 100000 INCREMENT BY 1;
