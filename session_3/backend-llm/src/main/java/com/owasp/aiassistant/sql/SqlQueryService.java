package com.owasp.aiassistant.sql;

import com.owasp.aiassistant.exception.ToolConnectivityException;
import com.owasp.aiassistant.tools.ToolErrorClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SqlQueryService {

    private static final String TOOL_NAME = "queryDatabase";

    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            "insert", "update", "delete", "drop", "alter", "truncate",
            "create", "grant", "revoke", "merge", "call", "execute"
    );

    private final JdbcTemplate jdbcTemplate;
    private final int maxRows;

    public SqlQueryService(JdbcTemplate jdbcTemplate, @Value("${app.sql.max-rows:100}") int maxRows) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxRows = maxRows;
    }

    public String executeReadOnlyQuery(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        if (normalized.isEmpty()) {
            return "SQL query must not be blank.";
        }

        String validationError = validateReadOnly(normalized);
        if (validationError != null) {
            return validationError;
        }

        String limitedSql = normalized.endsWith(";")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(limitedSql + " LIMIT " + maxRows);
            if (rows.isEmpty()) {
                return "Query returned no rows.";
            }

            return rows.stream()
                    .map(row -> row.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining("\n"));
        } catch (DataAccessException e) {
            if (ToolErrorClassifier.isConnectivityError(e)) {
                throw new ToolConnectivityException(TOOL_NAME, "Database query failed: " + e.getMessage(), e);
            }
            return "Database query failed: " + e.getMostSpecificCause().getMessage();
        }
    }

    public String describeSchema() {
        return """
                Available tables:
                - owasp_top_risks(id, year, rank, category, description)
                - security_controls(id, control_id, name, category, status, owner)
                - ingested_documents(id, document_id, title, source_path, version, chunk_count, content_hash, ingested_at, updated_at)
                """;
    }

    private String validateReadOnly(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            return "Only SELECT queries are allowed.";
        }
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (lower.matches(".*\\b" + keyword + "\\b.*")) {
                return "Forbidden SQL keyword: " + keyword;
            }
        }
        return null;
    }
}
