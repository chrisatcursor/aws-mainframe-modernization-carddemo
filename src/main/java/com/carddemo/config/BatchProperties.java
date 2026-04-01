package com.carddemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carddemo.batch")
public record BatchProperties(
        String dalytranInputDir,
        String rejectOutputDir,
        String statementTextDir,
        String statementHtmlDir) {
}
