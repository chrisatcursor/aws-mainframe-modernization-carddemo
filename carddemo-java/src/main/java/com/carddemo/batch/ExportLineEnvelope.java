package com.carddemo.batch;

/**
 * One logical export row: entity from the composite reader plus COBOL-aligned metadata (sequence and
 * timestamp) written on every CVEXPORT-style record.
 */
public record ExportLineEnvelope(int sequence, Object entity, String exportTimestamp) {}
