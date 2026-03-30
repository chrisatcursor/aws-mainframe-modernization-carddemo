# Java Target Conventions

Rules for all code generated in the `carddemo-java/` project.

## Project Structure

```
carddemo-java/src/main/java/com/carddemo/
  account/     Account, Customer entities + repos + services + controllers
  card/        Card, CardXref entities + repos + services + controllers
  transaction/ Transaction, TransactionCategoryBalance, DisclosureGroup,
               TransactionType, TransactionCategory entities + repos + services
  user/        User entity + repo + UserDetailsService + controllers
  report/      Report request/response DTOs + batch job launcher
  batch/       Spring Batch jobs (posting, interest, statements, import/export)
  config/      SecurityConfig, BatchConfig, WebConfig
  common/      Shared DTOs, utilities (DateValidator, etc.)
```

## Layering

Each migrated COBOL program maps to one or more of these layers:

1. **Entity** (`@Entity`) — mirrors a COBOL copybook record layout. Lives in the domain package.
2. **Repository** (`extends JpaRepository`) — one per entity. Named queries for VSAM alternate index paths.
3. **Service** (`@Service`, `@Transactional`) — business logic from the COBOL PROCEDURE DIVISION. One service per COBOL program or logical group.
4. **Controller** (`@Controller` or `@RestController`) — one per BMS screen/mapset. Maps CICS SEND/RECEIVE MAP to HTTP request/response.

## Naming

- Entity class names are singular nouns: `Account`, `Customer`, `Card`
- Repository interfaces: `AccountRepository`, `CardRepository`
- Service classes: `AccountService`, `PaymentService`, `InterestCalculationService`
- Controllers: `AccountController`, `CardController`, `UserController`
- Batch jobs: `TransactionPostingJob`, `StatementGenerationJob`
- DTOs: `AccountViewDto`, `CardUpdateRequest`

## Data Types

Follow the COBOL-to-Java cookbook for type mappings. Key rules:
- All financial amounts use `BigDecimal` with explicit scale
- COBOL PIC 9(n) numeric identifiers map to `Long` (preserving leading zeros via formatting, not storage)
- COBOL PIC X(n) maps to `String`
- Dates stored as `String` in entities (matching COBOL PIC X(10)), converted to `LocalDate` in service layer

## Testing

- Unit tests: JUnit 5 + Mockito for services
- Repository tests: `@DataJpaTest` with H2
- Controller tests: `@WebMvcTest` with MockMvc
- Batch tests: `@SpringBatchTest` with `JobLauncherTestUtils`
- Integration tests: `@SpringBootTest` with Testcontainers (PostgreSQL)

## Transaction Boundaries

- COBOL programs that REWRITE multiple files in sequence without SYNCPOINT: wrap in `@Transactional` to ensure atomicity
- Batch chunk processing: use Spring Batch chunk-oriented steps with commit-interval matching original batch behavior
- Bill payment and transaction posting are critical paths: always verify @Transactional boundaries in code review

## Security

- Passwords hashed with BCrypt (never plain text, unlike COBOL original)
- User types: `A` (admin) maps to `ROLE_ADMIN`, `U` (regular) maps to `ROLE_USER`
- Admin-only endpoints under `/admin/**`
- All API endpoints require authentication
