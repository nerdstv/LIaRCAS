package com.liarcas.ingestion.security;

/**
 * Authenticated tenant identity extracted from API key credentials.
 *
 * @param clientId authenticated client identifier
 * @param tenantId tenant identifier associated with the client
 */
public record TenantPrincipal(String clientId, String tenantId) {
}
