# CardDemo COBOL → Java Spring Boot Migration

This document describes the Java rewrite added at the repository root (`pom.xml`, `src/main/java`, `src/test/java`) for a subset of the AWS CardDemo mainframe application. Legacy COBOL sources remain under `app/cbl/`, `app/cpy/`, `app/bms/`, and `app/jcl/`.

## What Was Migrated (In Scope)

| COBOL | Role | Java equivalent |
|-------|------|-----------------|
| COACTVWC | Account view (CICS + CXACAIX + ACCTDAT + CUSTDAT) | `AccountViewService`, `GET /api/accounts/view` |
| COACTUPC | Account + customer update with optimistic concurrency | `AccountUpdateService`, `PUT /api/accounts` |
| COCRDLIC | Card list browse (7 rows, filters, cursor) | `CardListService`, `GET /api/cards/list` |
| COCRDSLC | Card detail by account + card | `CardDetailService`, `GET /api/cards/detail` |
| COCRDUPC | Card update + optional card number change | `CardUpdateService`, `PUT /api/cards` |
| CBTRN02C | Daily transaction posting | `TransactionPostingService` + Spring Batch job `transactionPostingJob` |
| CBACT04C | Interest from TCATBAL + DISCGRP | `InterestCalculationService` + `interestCalculationJob` |
| CBSTM03A | Statement output (text + HTML) | `StatementGenerationService` + `statementGenerationJob` (no CBSTM03B subprocess) |

### Copybooks → Persistence / DTOs

| Copybook | Java |
|----------|------|
| CVACT01Y (`ACCOUNT-RECORD`) | `com.carddemo.account.Account` → table `accounts` |
| CVCUS01Y (`CUSTOMER-RECORD`) | `com.carddemo.account.Customer` → `customers` |
| CVACT02Y (`CARD-RECORD`) | `com.carddemo.card.Card` → `cards` |
| CVACT03Y (`CARD-XREF-RECORD`) | `com.carddemo.card.CardXref` → `card_xrefs` |
| CVTRA05Y (`TRAN-RECORD`) | `com.carddemo.transaction.Transaction` → `transactions` |
| CVTRA01Y (`TRAN-CAT-BAL-RECORD`) | `com.carddemo.transaction.TransactionCategoryBalance` (composite key) → `transaction_category_balances` |
| CVTRA02Y (`DIS-GROUP-RECORD`) | `com.carddemo.transaction.DisclosureGroup` (composite key) → `disclosure_groups` |
| CVTRA06Y (`DALYTRAN-RECORD`) | `DalyTranRecordParser.ParsedDalyTran` + fixed 350-byte line layout for batch input |

BMS maps (`COACTVW`, `COACTUP`, `COCRDLI`, `COCRDSL`, `COCRDUP`) are not rendered as screens; request/response JSON replaces SEND/RECEIVE MAP.

### VSAM KSDS → JPA

- **ACCTDAT** → `AccountRepository`
- **CUSTDAT** → `CustomerRepository`
- **CARDDAT** (primary + CARDAIX-style queries) → `CardRepository` (`findByAccountId…`, `findByCardNumberGreaterThanEqual…`)
- **CXACAIX** (xref by account) → `CardXrefRepository.findByAccountId` (one xref per account in seed data; production VSAM may allow multiple)
- **TRANSACT** → `TransactionRepository`
- **TCATBALF** → `TransactionCategoryBalanceRepository`
- **DISCGRP** → `DisclosureGroupRepository`

### CICS → REST

- **COMMAREA / session flow** → Stateless REST; clients send account id, card number, and optimistic-lock versions where updates apply.
- **PF3 / XCTL** → Not modeled (out of scope for this API surface).

### JCL batch → Spring Batch

- **DALYTRAN / TRANFILE / DALYREJS** → `TransactionPostingService.postFromFile` reading lines from `${carddemo.batch.dalytran-input-dir}/DALYTRAN.txt` (override with job param `inputFile`), rejects to `reject-output-dir/DALYREJS.txt`.
- **CBACT04C** → `interestCalculationJob` with `parmDate` (10-char string used as transaction id prefix).
- **CBSTM03A STMTFILE / HTMLFILE** → `statementGenerationJob` writes under `statement-text-dir` and `statement-html-dir`.

Launch via REST (dev): `POST /api/batch/transaction-posting`, `POST /api/batch/interest-calculation`, `POST /api/batch/statement-generation`.

## Key Architectural Decisions

1. **H2 in-memory** for default local run (`application.yml`); swap `spring.datasource` for PostgreSQL in real deployments.
2. **Optimistic locking** (`@Version` on `Account`, `Customer`, `Card`) replaces CICS READ UPDATE / compare-old-values patterns from COACTUPC and COCRDUPC.
3. **Card list paging** uses a string `cursor` (next card number) instead of CICS STARTBR / READNEXT; page size fixed at **7** to match `WS-MAX-SCREEN-LINES` in COCRDLIC.
4. **DALYTRAN** input uses a **350-byte logical ASCII record** matching CVTRA06Y field order (documented in `DalyTranRecordParser`); this is a test-friendly stand-in for EBCDIC sequential files.
5. **CBSTM03A** is simplified: no TIOT/CBSTM03B call path; statements are built from JPA data in one JVM pass.

## Out of Scope / Differences

- **COSGN00C / menus / full CICS navigation** — not migrated here (security is a minimal permit-all for `/api/**` in `SecurityConfig`).
- **COBIL00C, COTRN\*, CORPT\*, user admin screens** — not in this branch.
- **CBEXPORT / CBIMPORT / CBTRN03C** — not implemented as Spring Batch jobs (Phase 2 tests cover posting, interest, sequential read patterns, and date utility behavior instead).
- **CBSTM03B** — not ported; file I/O subroutine behavior folded into `StatementGenerationService`.
- **Exact error message text** for all CICS RESP/RESP2 combinations — only key user-facing strings from COACTVWC were mirrored.
- **Multiple card xrefs per account** — `CardXrefRepository.findByAccountId` returns a single row; VSAM may allow more; would need a list endpoint and account-view adjustment.

## Verification

- Build and tests: `mvn test`
- Run: `mvn spring-boot-run` (demo data loads via `CardDemoDataLoader` when not using `test` profile)

## Branch

Work is on `cursor/carddemo-java-migration-7854` per cloud agent instructions.
