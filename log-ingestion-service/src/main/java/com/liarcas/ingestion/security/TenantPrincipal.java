package com.liarcas.ingestion.security;

public record TenantPrincipal(String clientId, String tenantId) {
}
