package com.carddemo.batch.service;

public record BatchPostingResult(
        int processedCount,
        int postedCount,
        int rejectedCount,
        int returnCode
) {
}
