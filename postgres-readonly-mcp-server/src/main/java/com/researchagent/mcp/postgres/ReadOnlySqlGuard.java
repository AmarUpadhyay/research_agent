package com.researchagent.mcp.postgres;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ReadOnlySqlGuard {

    private static final Pattern LEADING_COMMENTS = Pattern.compile("^(?:\\s|--.*?(?:\\R|$)|/\\*.*?\\*/)+", Pattern.DOTALL);
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "\\b(insert|update|delete|merge|upsert|alter|drop|create|truncate|grant|revoke|comment|copy|vacuum|analyze|refresh|reindex|cluster|call|do|listen|notify|set|reset)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DANGEROUS_FUNCTIONS = Pattern.compile(
            "\\b(pg_read_file|pg_read_binary_file|pg_ls_dir|pg_stat_file|pg_write_file|pg_logdir_ls|lo_import|lo_export|dblink_connect)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> ALLOWED_PREFIXES = Set.of("select", "with", "values", "show");

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be empty.");
        }

        String trimmed = sql.trim();
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("Semicolons are not allowed. Submit exactly one statement.");
        }

        String normalized = LEADING_COMMENTS.matcher(trimmed).replaceFirst("").stripLeading();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("SQL must contain a read-only statement.");
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean allowedPrefix = ALLOWED_PREFIXES.stream().anyMatch(lower::startsWith);
        if (!allowedPrefix) {
            throw new IllegalArgumentException("Only SELECT, WITH, VALUES, and SHOW statements are allowed.");
        }

        if (DANGEROUS_KEYWORDS.matcher(lower).find()) {
            throw new IllegalArgumentException("Write or session-changing SQL keywords are not allowed.");
        }

        if (DANGEROUS_FUNCTIONS.matcher(lower).find()) {
            throw new IllegalArgumentException("File system and privileged helper functions are not allowed.");
        }
    }
}
