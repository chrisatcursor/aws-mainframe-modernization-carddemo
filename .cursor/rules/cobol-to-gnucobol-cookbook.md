# COBOL to GNU COBOL Translation Cookbook

Reference for porting IBM Enterprise COBOL / CICS programs to GnuCOBOL.

## Compiler Differences

| IBM Enterprise COBOL | GnuCOBOL |
|---------------------|----------|
| `EXEC CICS ...` | Requires CICS preprocessor or replacement with native I/O |
| `EXEC SQL ...` | Use `ocesql` preprocessor or ODBC via `CALL` interface |
| `COPY member` (from PDS) | `COPY "filename.cpy"` from file system |
| Column 1-6 sequence numbers | Ignored by `cobc` (use `-free` for free-format) |
| `COMP-5` (native binary) | Supported natively |
| `DISPLAY ... UPON CONSOLE` | Works as-is |
| `ACCEPT ... FROM DATE/TIME` | Works as-is |
| `SERVICE RELOAD` | Not supported — remove |
| `JSON GENERATE/PARSE` | Not supported — use CALL to C library |

## CICS Replacement Strategy

Since GnuCOBOL has no native CICS runtime, CICS interactions must be replaced:

| CICS Function | GnuCOBOL Replacement |
|--------------|---------------------|
| `SEND MAP / RECEIVE MAP` | `DISPLAY` / `ACCEPT` for terminal; or CALL to curses/web library |
| `READ/WRITE/REWRITE/DELETE FILE` | Native COBOL file I/O (`SELECT ... ASSIGN TO`) or CALL to DB layer |
| `LINK / XCTL PROGRAM` | `CALL "program-name"` |
| `RETURN TRANSID` | Main loop with program dispatch table |
| `STARTBR / READNEXT / ENDBR` | `START`, `READ NEXT` on indexed file |
| `SYNCPOINT` | `COMMIT` if using ocesql; otherwise file CLOSE/OPEN |
| `COMMAREA` | Shared WORKING-STORAGE or LINKAGE SECTION parameters |
| `ASSIGN SYSID / USERID` | Environment variables via `ACCEPT FROM ENVIRONMENT` |

## VSAM to File System Mapping

| VSAM Type | GnuCOBOL File Organization |
|-----------|---------------------------|
| KSDS | `ORGANIZATION IS INDEXED`, `ACCESS MODE IS DYNAMIC` |
| ESDS | `ORGANIZATION IS SEQUENTIAL` |
| RRDS | `ORGANIZATION IS RELATIVE` |

File definitions use `SELECT ... ASSIGN TO "filename"` with actual file paths.
GnuCOBOL uses Berkeley DB or VBISAM as the indexed file handler.

## JCL to Shell Script Mapping

| JCL Construct | Shell Equivalent |
|--------------|-----------------|
| `//STEP EXEC PGM=program` | `./program` or `cobcrun program` |
| `//DD DSN=file,DISP=SHR` | Environment variable or command-line argument |
| `//SYSOUT=*` | stdout redirect |
| `//SYSIN DD *` inline data | heredoc or input file |
| `COND=(0,NE)` | `set -e` or `if [ $? -ne 0 ]` |
| `SORT FIELDS=...` | `sort` command or COBOL SORT verb |
| `IDCAMS REPRO` | `cp` or custom file copy utility |
| `IEBGENER` | `cp` |

## Build Commands

```bash
# Compile a single program (fixed-format)
cobc -x -o bin/PROGRAM app/cbl/PROGRAM.cbl -I app/cpy -I app/cpy-bms

# Compile as a callable module
cobc -m app/cbl/MODULE.cbl -I app/cpy

# Compile with debugging
cobc -x -g -debug app/cbl/PROGRAM.cbl -I app/cpy -I app/cpy-bms
```

## Known Incompatibilities to Flag

- BMS maps have no GnuCOBOL equivalent — screens must be reimplemented
- CICS HANDLE CONDITION / HANDLE AID — replace with explicit status checks
- CICS temporary storage / transient data queues — replace with files or IPC
- Packed decimal with odd byte counts may need alignment adjustment
- EBCDIC collation differs from ASCII — sort results may vary
