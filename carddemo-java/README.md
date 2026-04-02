# CardDemo Java (Phase 5 optional modules)

Spring Boot 3.4 / Java 21 implementation of Linear **CS-462** scope: pending authorization (IMS→relational), AUTHFRDS fraud reporting, transaction type admin + batch file job, JMS responders (COACCT01 / CODATE01 / simplified COPAUA0C).

## Run

- Default (PostgreSQL): set `spring.datasource.*` in `application.yml` or env vars, then `mvn spring-boot:run`.
- Tests: `mvn verify` (H2 + Flyway).
- JMS: `mvn -P jms spring-boot:run -Dspring-boot.run.profiles=jms` with Artemis (`docker compose -f docker-compose-artemis.yml up -d`).

## Security

First launch seeds `admin` / `password` and `user` / `password` (BCrypt). Change for any real deployment.

See [AGENTS.md](AGENTS.md) for architecture notes and Linear mapping.
