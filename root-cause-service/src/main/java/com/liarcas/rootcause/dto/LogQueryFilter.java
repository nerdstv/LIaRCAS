package com.liarcas.rootcause.dto;

import java.time.Instant;

/**
 * Caller-supplied filters for a log query.
 *
 * Intentionally excludes tenantId - tenant scope is derived exclusively from the
 * authenticated caller and is never accepted from request input.
 *
 * @param serviceName exact service name filter, or null to match any
 * @param level exact log level filter, or null to match any
 * @param from inclusive lower bound on timestamp, or null for no lower bound
 * @param to inclusive upper bound on timestamp, or null for no upper bound
 */
public record LogQueryFilter(
        String serviceName,
        String level,
        Instant from,
        Instant to
) {
}
