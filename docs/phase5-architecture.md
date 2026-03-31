# Phase 5 architecture decisions (Linear CS-462)

## IMS to PostgreSQL

- **Pending authorization (CIPAUSMY / CIPAUDTY):** Modeled as `pending_auth_summaries` (one row per `acct_id`, root segment) and `pending_auth_details` (child rows per authorization, surrogate `id`).
- **Date/time keys:** COBOL stores inverted Julian-style `auth_date_9c` and `auth_time_9c`; we persist these integers plus human-readable `auth_orig_date` / `auth_orig_time` for display and purge logic matching `CBPAUP0C`.
- **Purge rule (CBPAUP0C):** `day_diff = currentYyddd - (99999 - auth_date_9c)`; delete detail when `day_diff >= expiry_days` (default 5). Adjust summary counts; delete summary when both approved and declined counts are zero (COBOL compares `PA-APPROVED-AUTH-CNT` twice—treated as “both tallies empty” for summary removal).

## MQ replacement

- **Spring JMS + embedded Artemis** (`spring-boot-starter-artemis`, `spring.artemis.mode=embedded`) for local and CI.
- **Queue names:** `carddemo.auth.request`, `carddemo.auth.reply`, `carddemo.reply.date`, `carddemo.reply.acct`, `carddemo.error` (configurable via `carddemo.mq.*` in `application.yml`).
- **Message contract:** Request body is text: prefix `DATE` (date inquiry) or `ACCT` + 11-digit account id (account inquiry), or `AUTH` + pipe-delimited fields for the bridge (simplified from mainframe MQ payloads).

## DB2 transaction types vs PostgreSQL

- **Reuse** existing `transaction_types` (`type_cd`, `type_desc`) and `transaction_categories` (`type_cd`, `cat_cd`, `description`).
- **Align with DCLTRCAT:** Added `category_code` `CHAR(4)` NOT NULL on `transaction_categories` (backfilled from `cat_cd`) so COBOL-style category codes are addressable. `COBTUPDT` batch continues to target types; category maintenance uses existing tables with the new column.

## Fraud reporting (AUTHFRDS)

- Table `fraud_reports` mirrors `app/app-authorization-ims-db2-mq/ddl/AUTHFRDS.ddl` with PK `(card_num, auth_ts)`.
