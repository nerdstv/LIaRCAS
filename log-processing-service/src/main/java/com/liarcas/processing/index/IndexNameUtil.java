package com.liarcas.processing.index;

import java.util.regex.Pattern;

/**
 * Utility for generating and sanitizing Elasticsearch index names.
 * 
 * Index names in Elasticsearch must follow specific rules:
 * - Must be lowercase
 * - Cannot contain spaces or special characters except . - _
 * - Cannot start with . or -
 * - Cannot contain :
 * 
 * This utility generates tenant-scoped index names like liarcas-logs-tenant-001
 * and sanitizes tenantId to ensure valid index names.
 */
public class IndexNameUtil {

    // Avoid built-in Elastic data stream templates that match logs-*-*.
    private static final String INDEX_PREFIX = "liarcas-logs";
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9._-]");
    private static final Pattern LEADING_INVALID = Pattern.compile("^[._-]+");

    /**
     * Generate a tenant-scoped index name.
     * 
     * @param tenantId the tenant identifier
    * @return a sanitized index name like liarcas-logs-tenant-001
     */
    public static String getTenantIndexName(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or blank");
        }

        String sanitizedTenantId = sanitizeIndexName(tenantId);
        return INDEX_PREFIX + "-" + sanitizedTenantId;
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
    private static String sanitizeIndexName(String input) {
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
