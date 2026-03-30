# CardDemo Migration Conventions

## Target Architectures

This migration has two target tracks running in parallel:

### Track 1: GNU COBOL (Replatform)
- Compile all COBOL sources with GnuCOBOL (cobc)
- Replace CICS/VSAM dependencies with open-source equivalents
- Replace BMS maps with terminal or web-based screen handling
- Replace JCL with shell scripts or a batch orchestrator
- Data layer: migrate VSAM to PostgreSQL or SQLite flat files
- Goal: functional parity on Linux with no proprietary runtime

### Track 2: Java (Rewrite)
- Java 21+ with Spring Boot 3.x
- REST API layer replacing CICS transaction interface
- JPA/Hibernate for data access (PostgreSQL)
- Business logic translated module-by-module from COBOL
- Batch processing via Spring Batch (replacing JCL/batch COBOL)
- Web UI replacing BMS screens (Thymeleaf or React)

## Naming Conventions

### GNU COBOL track
- Source files: keep original names, lowercase extension `.cbl`
- Copybooks: keep original names, lowercase extension `.cpy`
- Build artifacts: `build/gnucobol/`
- Tests: `tests/gnucobol/`

### Java track
- Package root: `com.carddemo`
- Sub-packages by domain: `.account`, `.card`, `.transaction`, `.user`, `.report`, `.batch`
- Source: `src/main/java/com/carddemo/`
- Tests: `src/test/java/com/carddemo/`
- Resources: `src/main/resources/`

## Migration Rules

1. **One module per PR.** Each migrated COBOL program becomes its own PR.
2. **Tests first.** Behavioral tests describing current behavior must exist before migration code is written.
3. **No feature changes during migration.** Preserve existing behavior exactly. Improvements go on a post-migration backlog.
4. **Copybook mappings are shared.** Copybook-to-class/struct mappings are defined once and reused across all modules.
5. **Document deviations.** Any behavior that cannot be replicated identically must be documented with a rationale.

## Complexity Classification

- **S (Small):** Leaf utility programs, simple batch readers/writers, stateless copybooks. ~1 session.
- **M (Medium):** Single-screen CICS programs with local VSAM I/O. ~1-2 sessions.
- **L (Large):** Multi-screen workflows, cross-file transactions, complex business logic. ~2-4 sessions.
- **XL (Extra Large):** Core transaction processing, authorization flows, multi-system integration (IMS/DB2/MQ). Requires human architecture decisions first.
