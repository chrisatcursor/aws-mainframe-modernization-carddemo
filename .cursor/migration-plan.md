# CardDemo Migration Plan

## Master Plan: COBOL → GNU COBOL + Java

**Project**: AWS Mainframe Modernization CardDemo
**Source**: IBM Enterprise COBOL with CICS/VSAM on z/OS
**Targets**: GNU COBOL (replatform) and Java 21 / Spring Boot 3.x (rewrite)
**Approach**: Full rewrite with agent-assisted automation (per Playbook §4)

---

## Codebase Summary

| Category | Count |
|----------|-------|
| COBOL programs | 44 (30 core + 14 optional modules) |
| Copybooks | 62 |
| JCL files | 55 |
| BMS screen maps | 21 |
| CICS CSD definitions | 4 |
| Assembler sources | 2 |
| IMS/DB2/MQ artifacts | 17 |
| Data files | 23 |
| Shell scripts | 9 |

### Core Modules (app/cbl/) — 30 programs

**Online (CICS) Programs:**
- COSGN00C — Sign-on / authentication
- COMEN01C — Main menu
- COADM01C — Admin menu
- COACTUPC — Account update
- COACTVWC — Account view
- COBIL00C — Bill pay
- COCRDLIC — Credit card list
- COCRDSLC — Credit card detail select
- COCRDUPC — Credit card update
- COTRN00C — Transaction menu
- COTRN01C — Transaction list
- COTRN02C — Transaction detail
- COUSR00C — User management menu
- COUSR01C — User list
- COUSR02C — User update
- COUSR03C — User add
- CORPT00C — Report menu
- COBSWAIT — Batch wait screen

**Batch Programs:**
- CBACT01C — Account file read
- CBACT02C — Account file process
- CBACT03C — Account cross-ref process
- CBACT04C — Account interest calc
- CBCUS01C — Customer file process
- CBTRN01C — Transaction file read
- CBTRN02C — Transaction post
- CBTRN03C — Transaction report
- CBSTM03A — Statement generation (part A)
- CBSTM03B — Statement generation (part B)
- CSUTLDTC — Date utility
- CBEXPORT — Data export
- CBIMPORT — Data import

### Optional Modules
- **IMS/DB2/MQ Authorization** (8 programs) — XL complexity, deferred
- **DB2 Transaction Type** (3 programs) — XL complexity, deferred
- **VSAM/MQ** (2 programs) — L complexity, deferred

---

## Phase Plan

### Phase 0: Scaffolding and Inventory (Current)
- [x] Commit migration playbook
- [x] Create migration branch
- [x] Set up .cursor/rules with migration conventions and cookbooks
- [x] Set up .cursor/agents with subagent definitions
- [x] Run inventory agent on core modules
- [ ] Create Linear project with backlog from inventory
- [ ] Define copybook-to-class mapping reference

### Phase 1: Foundation (Weeks 1-2)
**Goal**: Shared infrastructure that all modules depend on.

#### Track 1: GNU COBOL
- Set up GnuCOBOL build system (Makefile + cobc)
- Convert copybooks (remove IBM-specific syntax)
- Build VSAM-to-indexed-file data layer
- Create CICS replacement dispatcher (main loop + program dispatch)

#### Track 2: Java
- Initialize Spring Boot project (pom.xml, application.yml)
- Create JPA entities from copybook record layouts
- Create base repository interfaces
- Set up Spring Security (from COSGN00C auth model)
- Create common DTOs from shared copybooks

**Subplans:**
- S1.1: Copybook conversion (all 30 data copybooks) — S complexity, parallelizable
- S1.2: BMS-to-template mapping (17 screen copybooks) — M complexity
- S1.3: Data file setup (VSAM → PostgreSQL schema + indexed files) — M complexity
- S1.4: Build system setup (both tracks) — S complexity

### Phase 2: Leaf Units (Weeks 2-3)
**Goal**: Batch utilities and simple programs with no CICS dependency.

**Migration order:**
1. CSUTLDTC (date utility) — S, no CICS, no file I/O
2. CBACT01C (account file read) — S, sequential file read only
3. CBCUS01C (customer file read) — S, sequential file read only
4. CBTRN01C (transaction file read) — S, sequential file read only
5. CBEXPORT / CBIMPORT — M, multi-file I/O but straightforward
6. CBACT02C-04C (account processing chain) — M, business logic
7. CBTRN02C-03C (transaction posting and reporting) — M-L
8. CBSTM03A/B (statement generation) — L, multi-step batch

**Subagent sequence per module:** Inventory → Test Gen → Migrate → Modernize → Security Scan

### Phase 3: Simple Containers (Weeks 3-5)
**Goal**: Single-screen CICS programs.

**Migration order (by dependency — login first, then menus, then leaves):**
1. COSGN00C (sign-on) — M, authentication flow
2. COMEN01C (main menu) — M, navigation hub
3. COADM01C (admin menu) — M, admin navigation
4. COACTVWC (account view) — M, read-only display
5. COCRDLIC (card list) — M, browse/list pattern
6. COCRDSLC (card detail) — M, detail view
7. COUSR01C (user list) — M, browse pattern
8. CORPT00C (report menu) — S, simple launcher

### Phase 4: Complex Workflows (Weeks 5-8)
**Goal**: Multi-screen flows and update transactions.

1. COACTUPC (account update) — L, full CRUD with validation
2. COCRDUPC (card update) — L, full CRUD with validation
3. COUSR02C / COUSR03C (user update/add) — L, user management
4. COTRN00C-02C (transaction flow) — L, multi-screen workflow
5. COBIL00C (bill pay) — L, financial transaction
6. COUSR00C (user menu) — M, depends on user CRUD being done

### Phase 5: Optional Modules (Weeks 8-10, if in scope)
- IMS/DB2/MQ authorization module — XL, requires architecture decisions
- DB2 transaction type module — XL, requires DB2-to-JPA mapping
- VSAM/MQ module — L, requires MQ replacement decision

### Phase 6: Integration and Validation (Weeks 10-12)
- End-to-end testing across all migrated modules
- Performance benchmarking vs. original
- Security audit of complete system
- Documentation and runbook creation

---

## Subagent Roster

| Agent | Role | When to Use |
|-------|------|-------------|
| **inventory-agent** | Crawl and classify modules | Phase 0, start of each batch |
| **test-generation-agent** | Write behavioral tests | Before every module migration |
| **migration-agent** | Convert COBOL to target | Core migration work |
| **modernization-agent** | Refactor to idiomatic code | After every module migration |
| **security-scanning-agent** | Audit for vulnerabilities | After modernization pass |

## Metrics to Track

- Modules migrated / total
- Test pass rate per batch
- PR review time
- Defects found post-migration
- Lines of code: source vs target
- Estimated vs actual complexity per module

---

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| BMS screen semantics lost in translation | HIGH | Capture screen behavior in tests before removing BMS |
| EBCDIC/ASCII data conversion errors | HIGH | Validate with sample data roundtrip tests |
| CICS transaction semantics differ from REST | MEDIUM | Document behavioral differences, get stakeholder sign-off |
| Batch job sequencing lost without JCL | MEDIUM | Shell scripts preserve step ordering and condition codes |
| IMS/DB2/MQ modules require proprietary middleware | HIGH | Defer to Phase 5, may require architecture decision |
| Packed decimal precision loss | HIGH | Use BigDecimal in Java, verify with financial test cases |
