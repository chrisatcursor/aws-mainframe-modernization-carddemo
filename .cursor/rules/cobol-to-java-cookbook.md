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
