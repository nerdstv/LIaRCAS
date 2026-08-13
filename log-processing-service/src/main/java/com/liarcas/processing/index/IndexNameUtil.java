package com.liarcas.processing.index;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates tenant-scoped Elasticsearch index names from a configurable pattern.
 * 
 * The pattern must include the placeholder {tenantId}. The tenant value is sanitized
 * before substitution and the resolved index name is validated against Elasticsearch
 * naming constraints.
 *
 * Index names in Elasticsearch must follow these rules:
 * - Must be lowercase
 * - Cannot contain spaces or special characters except . - _
 * - Cannot start with . or - or _
 * - Cannot contain :
 */
@Component
public class IndexNameUtil {

    private static final String TENANT_PLACEHOLDER = "{tenantId}";
    private static final int MAX_INDEX_NAME_LENGTH = 255;
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9._-]");
    private static final Pattern LEADING_INVALID = Pattern.compile("^[._-]+");

    private final String indexPattern;

    public IndexNameUtil(@Value("${liarcas.elasticsearch.index-pattern:liarcas-logs-{tenantId}}") String indexPattern) {
        this.indexPattern = indexPattern;
        validatePattern(indexPattern);
    }

    /**
     * Generate a tenant-scoped index name.
     * 
     * @param tenantId the tenant identifier
     * @return a sanitized index name like liarcas-logs-tenant-001
     */
    public String getTenantIndexName(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or blank");
        }

        String sanitizedTenantId = sanitizeIndexName(tenantId);
        String tenantIndexName = indexPattern.replace(TENANT_PLACEHOLDER, sanitizedTenantId);
        validateResolvedIndexName(tenantIndexName);
        return tenantIndexName;
    }

    private void validatePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalStateException("liarcas.elasticsearch.index-pattern cannot be null or blank");
        }

        if (!pattern.contains(TENANT_PLACEHOLDER)) {
            throw new IllegalStateException(
                    "liarcas.elasticsearch.index-pattern must include " + TENANT_PLACEHOLDER
            );
        }

        String resolvedExample = pattern.replace(TENANT_PLACEHOLDER, "tenant-validation");
        validateResolvedIndexName(resolvedExample);
    }

    private void validateResolvedIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalStateException("Resolved Elasticsearch index name cannot be blank");
        }

        if (indexName.length() > MAX_INDEX_NAME_LENGTH) {
            throw new IllegalStateException("Resolved Elasticsearch index name exceeds 255 characters");
        }

        if (LEADING_INVALID.matcher(indexName).find()) {
            throw new IllegalStateException("Elasticsearch index name cannot start with '.', '_' or '-'");
        }

        if (indexName.indexOf(':') >= 0) {
            throw new IllegalStateException("Elasticsearch index name cannot contain ':'");
        }

        if (INVALID_CHARS.matcher(indexName).find()) {
            throw new IllegalStateException(
                    "Elasticsearch index name contains invalid characters. Allowed: a-z, 0-9, '.', '-', '_'."
            );
        }
    }

    /**
     * Sanitize a string to be safe for use as an Elasticsearch index name.
     * 
     * Rules applied:
     * - Convert to lowercase
     * - Replace invalid characters with hyphens
     * - Remove leading invalid characters
     * - Max length enforced by caller
     * 
     * @param input the string to sanitize
     * @return a sanitized index name component
     */
    private String sanitizeIndexName(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be null or blank");
        }

        // Convert to lowercase
        String normalized = input.toLowerCase();

        // Replace invalid characters with hyphens
        // Valid: a-z, 0-9, . (dot), - (hyphen), _ (underscore)
        normalized = INVALID_CHARS.matcher(normalized).replaceAll("-");

        // Remove leading invalid characters (. - _)
        normalized = LEADING_INVALID.matcher(normalized).replaceAll("");

        // If result is empty after sanitization, use a default
        if (normalized.isBlank()) {
            normalized = "unknown";
        }

        return normalized;
    }
}
