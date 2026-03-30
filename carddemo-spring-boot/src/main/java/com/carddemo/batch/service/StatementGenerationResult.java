package com.carddemo.batch.service;

import java.nio.file.Path;
import java.util.List;

public record StatementGenerationResult(
        int statementCount,
        List<String> textStatements,
        List<String> htmlStatements,
        Path textStatementPath,
        Path htmlStatementPath,
        List<String> accountIds
) {
}
