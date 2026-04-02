# CardDemo Java — agent notes

## Phase 5 architecture decisions (Linear CS-462)

Resolved for this repository:

1. **IMS hierarchy → relational schema**  
   Pending authorization IMS segments (`CIPAUSMY` root, `CIPAUDTY` child) are modeled as PostgreSQL tables `pending_auth_summary` and `pending_auth_detail` with a foreign key from detail to summary (`acct_id`). See Flyway `V4__phase5_optional_module.sql`.

2. **MQ replacement**  
   Optional IBM MQ flows use **Spring JMS** behind `carddemo.features.jms-enabled=true`. Queue names are configurable in `application-jms.yml`. Artemis is supported via `docker-compose-artemis.yml` for local smoke tests.

3. **DB2 → PostgreSQL**  
   DB2 DDL under `app/app-transaction-type-db2/ddl/` and `app/app-authorization-ims-db2-mq/ddl/` is translated to PostgreSQL types in Flyway migrations (e.g. `AUTHFRDS` → `auth_fraud_reports`, `TRANSACTION_TYPE` → `transaction_types`).

## Linear traceability

| Linear | COBOL | Java area |
|--------|-------|-----------|
| CS-499 | COPAUS0C | `PendingAuthController` list |
| CS-500 | COPAUS1C | `PendingAuthController` detail + fraud toggle |
| CS-501 | COPAUS2C | `FraudReportService` |
| CS-502 | CBPAUP0C | `PendingAuthPurgeJobConfig` |
| CS-503 | COPAUA0C | `AuthorizationBridgeListener` |
| CS-504 | DBUNLDGS | `PendingAuthExportJobConfig` |
| CS-505 | PAUDBLOD | `PendingAuthImportJobConfig` |
| CS-506 | PAUDBUNL | Same export job (dual output files) |
| CS-507–509 | COTRTLIC / COTRTUPC / COBTUPDT | `TransactionTypeAdminController`, `TransactionTypeFileUpdateJobConfig` |
| CS-510 | COACCT01 | `AccountInquiryJmsListener` |
| CS-511 | CODATE01 | `SystemDateJmsListener` |
