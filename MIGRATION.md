# CardDemo COBOL to Spring Boot Migration

## Scope implemented

This migration introduces a new Java 21 / Spring Boot 3 module:

- `carddemo-spring-boot/`

Implemented scope covers the requested online and batch targets:

- Online (CICS program parity surface):
  - `COACTVWC` → account view API/service
  - `COACTUPC` → account update API/service with validation and optimistic version guard
  - `COCRDLIC` → card list API/service with account and card filters + paging
  - `COCRDSLC` → card detail API/service by account + card key
  - `COCRDUPC` → card update API/service with validation and change conflict checks
- Batch:
  - `CBTRN02C` → transaction posting batch service + rejection persistence
  - `CBACT04C` → interest calculation batch service + system transaction generation
  - `CBSTM03A` (`CBSTM03B` behavior absorbed) → statement generation batch service (text + HTML outputs in-memory and persisted artifact files)

## COBOL-to-Java mapping patterns

### Copybooks → entities/records

| Copybook | Java target |
|---|---|
| `CVACT01Y` | `Account` |
| `CVCUS01Y` | `Customer` |
| `CVACT02Y` | `Card` |
| `CVACT03Y` | `CardXref` |
| `CVTRA06Y` | `DailyTransactionRecord` |
| `CVTRA05Y` | `TransactionRecord` |
| `CVTRA01Y` | `TransactionCategoryBalance` + `TransactionCategoryBalanceId` |
| `CVTRA02Y` | `DisclosureGroup` + `DisclosureGroupId` |
| `COCOM01Y` | `SessionContext` |

### VSAM KSDS / AIX → Spring Data JPA

| COBOL file/index behavior | Java mapping |
|---|---|
| keyed reads/writes/rewrite on account/card/customer | `JpaRepository` CRUD methods |
| alternate-index lookup by account/customer/card | repository query methods (`findByAccountId...`, `findByCustomerId...`, `findByAccountIdAndCardNumber`) |
| browse/list patterns (`STARTBR`/`READNEXT`) | `Pageable` queries + sorted list APIs |

### CICS programs → REST controllers/services

| COBOL program | REST/controller mapping |
|---|---|
| `COACTVWC` | `GET /api/accounts/{accountId}` |
| `COACTUPC` | `PUT /api/accounts/{accountId}` |
| `COCRDLIC` | `GET /api/cards` |
| `COCRDSLC` | `GET /api/cards/{cardNumber}?accountId=...` |
| `COCRDUPC` | `PUT /api/cards/{cardNumber}?accountId=...` |

Validation/error handling is centralized via `LegacyValidation` and `GlobalExceptionHandler` with JSON error payloads.

### JCL batch steps → Spring batch-style services + API launchers

| JCL / COBOL step | Java mapping |
|---|---|
| `POSTTRAN.jcl` / `CBTRN02C` | `TransactionPostingBatchService` + `POST /api/batch/posting` |
| `INTCALC.jcl` / `CBACT04C` | `InterestCalculationBatchService` + `POST /api/batch/interest` |
| `CREASTMT.JCL` / `CBSTM03A`+`03B` | `StatementGenerationBatchService` + `POST /api/batch/statements` |

## Key architectural decisions

1. **New module in-place**: migration is isolated under `carddemo-spring-boot` without mutating original COBOL sources.
2. **Behavior-first data foundation**: copybook-driven entities and fixed-width codecs were built before workflow APIs.
3. **H2-backed deterministic local runtime**: allows compile/start/test in cloud VM without external dependencies.
4. **Financial precision via `BigDecimal`**: all monetary fields/operations use explicit decimal math.
5. **ASCII bootstrap loader**: sample data from `app/data/ASCII` is ingested at startup for repeatable tests.
6. **Conflict protections**:
   - account updates: optimistic version check
   - card updates: original-value compare before commit

## Verification evidence

Executed successfully in the new module:

- Compile:
  - `./mvnw -q -DskipTests compile`
- Tests:
  - `./mvnw -q test`
  - includes fixed-width/data-loader tests, behavior tests, integration API tests, and batch service tests
- Startup:
  - `./mvnw -q spring-boot:run`
  - health check: `curl http://localhost:8080/api/health` returned `{"status":"UP",...}`

## Out of scope / not yet migrated

- Full BMS screen emulation and PF-key UX parity.
- Optional CardDemo subsystems not in requested scope (authorization/IMS/DB2/MQ side modules).
- Full Spring Batch `Job`/`Step` orchestration objects (`JobRepository`-driven pipelines) were not introduced; equivalent service-layer batch logic and launch endpoints are implemented.
- Exact legacy statement formatting layout/byte-for-byte rendering parity is not complete; business content generation is implemented.
