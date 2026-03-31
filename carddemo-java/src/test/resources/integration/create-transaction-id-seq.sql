-- Native query in TransactionRepository.getNextTransactionId(); Flyway is off for tests.
CREATE SEQUENCE IF NOT EXISTS transaction_id_seq START WITH 100000 INCREMENT BY 1;
