# COBOL to Java Translation Cookbook

Reference mapping for converting COBOL idioms to Java equivalents.

## Data Types

| COBOL | Java |
|-------|------|
| `PIC X(n)` | `String` (padded/truncated to length n) |
| `PIC 9(n)` | `int` or `long` (depending on n) |
| `PIC 9(n)V9(m)` | `BigDecimal` with scale m |
| `PIC S9(n) COMP-3` | `BigDecimal` (packed decimal) |
| `PIC S9(n) COMP` | `int` or `long` (binary) |
| `88-level` | `enum` or boolean constant |
| `OCCURS n TIMES` | `List<T>` or array of size n |
| `REDEFINES` | Union type or separate parsing methods |

## File I/O

| COBOL (VSAM) | Java |
|--------------|------|
| `OPEN INPUT file` | `try (var reader = repository.openRead())` |
| `READ file INTO ws-record` | `Optional<Record> record = repository.findById(key)` |
| `WRITE record FROM ws-record` | `repository.save(entity)` |
| `REWRITE record FROM ws-record` | `repository.save(entity)` (JPA merge) |
| `DELETE file RECORD` | `repository.deleteById(key)` |
| `START file KEY >= ws-key` | `repository.findByKeyGreaterThanEqual(key)` |
| Sequential READ NEXT | `Iterator<T>` or `Stream<T>` |

## CICS Commands

| CICS | Spring Boot |
|------|-------------|
| `EXEC CICS SEND MAP` | Controller returns view/template |
| `EXEC CICS RECEIVE MAP` | `@PostMapping` with `@RequestBody` or form binding |
| `EXEC CICS RETURN TRANSID` | Session state + redirect |
| `EXEC CICS LINK PROGRAM` | Service method call or `@Autowired` dependency |
| `EXEC CICS XCTL PROGRAM` | Controller forward/redirect |
| `EXEC CICS READ FILE` | JPA repository call |
| `EXEC CICS STARTBR / READNEXT / ENDBR` | Paginated query |
| `EXEC CICS SYNCPOINT` | `@Transactional` boundary |
| `COMMAREA` | DTO passed between service methods |

## Control Flow

| COBOL | Java |
|-------|------|
| `PERFORM paragraph` | Private method call |
| `PERFORM paragraph THRU exit-paragraph` | Method call (flatten THRU ranges) |
| `PERFORM VARYING` | `for` loop |
| `PERFORM UNTIL` | `while` loop |
| `EVALUATE TRUE` | `switch` expression or `if/else` chain |
| `GO TO paragraph` | Refactor to method calls (eliminate GOTOs) |
| `STOP RUN` | `System.exit()` or return from main |

## Copybook to Class Mapping

Each copybook (`.cpy`) becomes a Java record or class:
- Data-only copybooks → Java `record` with validation
- Copybooks with 88-levels → `record` + `enum` for flag fields
- BMS copybooks → Request/Response DTOs for the corresponding controller
- Shared copybooks (COCOM01Y, CSMEN02Y, etc.) → shared `common` package classes

### Concrete Entity Mapping Table

| Copybook | VSAM File | Java Entity | Package | Table |
|----------|-----------|-------------|---------|-------|
| `CVACT01Y` | ACCTDAT | `Account` | `com.carddemo.account` | `accounts` |
| `CVCUS01Y` | CUSTDAT | `Customer` | `com.carddemo.account` | `customers` |
| `CVACT02Y` | CARDDAT | `Card` | `com.carddemo.card` | `cards` |
| `CVACT03Y` | CCXREF / CXACAIX | `CardXref` | `com.carddemo.card` | `card_xrefs` |
| `CVTRA05Y` | TRANSACT | `Transaction` | `com.carddemo.transaction` | `transactions` |
| `CVTRA01Y` | TCATBALF | `TransactionCategoryBalance` | `com.carddemo.transaction` | `transaction_category_balances` |
| `CSUSR01Y` | USRSEC | `User` | `com.carddemo.user` | `users` |
| `CVTRA02Y` | DISCGRP | `DisclosureGroup` | `com.carddemo.transaction` | `disclosure_groups` |
| `CVTRA03Y` | TRANTYPE | `TransactionType` | `com.carddemo.transaction` | `transaction_types` |
| `CVTRA04Y` | TRANCATG | `TransactionCategory` | `com.carddemo.transaction` | `transaction_categories` |

### VSAM Alternate Index to JPA Query Mapping

| VSAM AIX Path | Purpose | JPA Repository Method |
|---------------|---------|----------------------|
| CARDAIX | Cards by account | `CardRepository.findByAccountId(Long)` |
| CXACAIX | Card xrefs by account | `CardXrefRepository.findByAccountId(Long)` |
| — | Card xrefs by customer | `CardXrefRepository.findByCustomerId(Long)` |
| — | Transactions by card | `TransactionRepository.findByCardNumber(String, Pageable)` |
| — | Category balances by acct | `TransactionCategoryBalanceRepository.findByIdAcctId(Long)` |

### Program to Service/Controller Mapping

| COBOL Program | Java Target | Notes |
|---------------|-------------|-------|
| COSGN00C | `SecurityConfig` + `UserDetailsService` | Spring Security auth |
| COMEN01C | `MenuController` | User menu routing |
| COADM01C | `AdminController` | Admin menu routing |
| COACTVWC | `AccountController.view()` | Read-only account view |
| COACTUPC | `AccountController.update()` + `AccountValidator` | XL — break into 3 classes |
| COCRDLIC | `CardController.list()` | Paginated with role filter |
| COCRDSLC | `CardController.detail()` | Detail view |
| COCRDUPC | `CardController.update()` | Update with confirmation |
| COTRN00C | `TransactionController.list()` | Paginated browse |
| COTRN01C | `TransactionController.detail()` | Detail view |
| COTRN02C | `TransactionController.create()` + `TransactionService` | Cross-file write |
| COBIL00C | `PaymentService.payBill()` | Critical path |
| CORPT00C | `ReportController` + batch launcher | Triggers Spring Batch job |
| COUSR00C-03C | `UserController` CRUD | Full user management |
| CBTRN02C | `TransactionPostingJob` | Spring Batch chunk |
| CBACT04C | `InterestCalculationJob` | Spring Batch tasklet |
| CBSTM03A/B | `StatementGenerationJob` | Spring Batch + template |
| CBEXPORT/CBIMPORT | `DataExportJob` / `DataImportJob` | Spring Batch multi-file |

### Shared Copybook to Common Class Mapping

| Copybook | Java Target | Package | Purpose |
|----------|-------------|---------|---------|
| `COCOM01Y` | `SessionContext` | `com.carddemo.common` | CICS COMMAREA replacement: navigation and user context |
| `COMEN02Y` | `MenuOption` + `MenuConfig` | `com.carddemo.common` | Main menu option definitions |
| `COADM02Y` | Reuses `MenuOption` / `MenuConfig` | `com.carddemo.common` | Admin menu option definitions |
| `CSMSG01Y` | `CommonMessages` | `com.carddemo.common` | Application message constants |
| `CSMSG02Y` | `AbendInfo` | `com.carddemo.common` | Structured error/abend data |
| `COTTL01Y` | `ScreenTitles` | `com.carddemo.common` | Application title constants |
| `CSDAT01Y` | `DateTimeHelper` | `com.carddemo.common` | Date/time formatting utility |
| `CSSTRPFY` | `StringPaddingUtil` | `com.carddemo.common` | Fixed-width string operations |
| `CSSETATY` | Absorbed into Thymeleaf templates | n/a | BMS attribute bytes: no Java equivalent |

### BMS Map to Thymeleaf Template Mapping

| BMS Map (copybook) | Thymeleaf Template | Controller |
|--------------------|-------------------|------------|
| `COSGN00` | `login.html` | Spring Security default |
| `COMEN01` | `menu.html` | `MenuController` |
| `COADM01` | `admin/dashboard.html` | `AdminController` |
| `COACTVW` | `account/view.html` | `AccountController` |
| `COACTUP` | `account/edit.html` | `AccountController` |
| `COCRDSL` | `card/detail.html` | `CardController` |
| `COCRDLI` | `card/list.html` | `CardController` |
| `COCRDUP` | `card/edit.html` | `CardController` |
| `COTRN00` | `transaction/list.html` | `TransactionController` |
| `COTRN01` | `transaction/detail.html` | `TransactionController` |
| `COTRN02` | `transaction/add.html` | `TransactionController` |
| `COBIL00` | `payment/bill.html` | `PaymentController` |
| `CORPT00` | `report/submit.html` | `ReportController` |
| `COUSR00` | `admin/user/list.html` | `UserController` |
| `COUSR01` | `admin/user/add.html` | `UserController` |
| `COUSR02` | `admin/user/edit.html` | `UserController` |
| `COUSR03` | `admin/user/delete.html` | `UserController` |
