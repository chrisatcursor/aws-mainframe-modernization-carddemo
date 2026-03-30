# AGENTS.md

## Cursor Cloud specific instructions

### Codebase overview

CardDemo is a **mainframe COBOL/CICS credit card management application** designed by AWS to showcase mainframe migration and modernization. It is **not** a web application, microservice, or modern language project. There are no `package.json`, `requirements.txt`, Docker, or similar dependency files.

### What can run locally

- **GnuCOBOL syntax checking**: Batch COBOL programs (those without `EXEC CICS`, `EXEC SQL`, or `EXEC DLI`) can be syntax-checked using `cobc` with `--std=ibm-strict -fsyntax-only`.
- **Compile command**: `cobc -I app/cpy/ -I app/cpy-bms/ --std=ibm-strict -fsyntax-only <file.cbl>`
- For optional module copybooks, add `-I app/app-authorization-ims-db2-mq/cpy/` or `-I app/app-transaction-type-db2/cpy/` as needed.

### What cannot run locally

- **CICS online programs** (prefixed `CO*.cbl`): These use `EXEC CICS` and require CICS copybooks (`DFHAID`, `DFHBMSCA`) not available in GnuCOBOL. They need an IBM mainframe or AWS Mainframe Modernization (M2) runtime.
- **DB2 programs**: Use `EXEC SQL` and require DB2 preprocessor and `SQLCA` copybook.
- **IMS programs**: Use `EXEC DLI` and require IMS preprocessor.
- **Full application runtime**: Requires CICS transaction processing, VSAM datasets, and JCL batch engine — none available on Linux.

### Batch programs that compile cleanly with GnuCOBOL

`CBACT01C`, `CBACT02C`, `CBACT03C`, `CBACT04C`, `CBCUS01C`, `CBTRN01C`, `CBTRN02C`, `CBTRN03C`, `COBSWAIT`, `CSUTLDTC`, `CBSTM03B`

### Key directories

- `app/cbl/` — Core COBOL programs (31 files)
- `app/cpy/` — Core copybooks (30 files)
- `app/cpy-bms/` — BMS screen copybooks
- `app/bms/` — BMS screen map definitions
- `app/jcl/` — JCL batch job definitions
- `app/data/` — Sample data (EBCDIC + ASCII)
- `app/app-*/` — Optional modules (DB2, IMS, MQ)
- `scripts/` — Shell helpers for mainframe upload/compile

### No automated tests

This codebase has no automated test suite. Validation is done via COBOL syntax checking with GnuCOBOL.

### No lint, build, or dev server

There are no lint tools, build systems, or development servers. The `scripts/local_compile.sh` file references the `cobc` command for local compilation. See the README for full installation/deployment instructions targeting a mainframe environment.
