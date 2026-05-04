package com.liarcas.ingestion.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApiKeyTenantResolver {

    private final Map<String, TenantPrincipal> tenantsByApiKey;

    public ApiKeyTenantResolver(ApiKeyAuthProperties authProperties) {
        if (authProperties.getClients().isEmpty()) {
            throw new IllegalStateException("At least one auth client must be configured");
        }

        Map<String, TenantPrincipal> configuredClients = new HashMap<>();

        for (ApiKeyAuthProperties.Client client : authProperties.getClients()) {
            if (client.getClientId() == null || client.getClientId().isBlank()) {
                throw new IllegalStateException("Each configured auth client must define client-id");
            }
            if (client.getApiKey() == null || client.getApiKey().isBlank()) {
                throw new IllegalStateException("Each configured auth client must define api-key");
            }
            if (client.getTenantId() == null || client.getTenantId().isBlank()) {
                throw new IllegalStateException("Each configured auth client must define tenant-id");
            }

            TenantPrincipal previous = configuredClients.put(
                    client.getApiKey(),
                    new TenantPrincipal(client.getClientId(), client.getTenantId())
            );

            if (previous != null) {
                throw new IllegalStateException("Duplicate api-key configured for tenant resolution");
            }
        }

        this.tenantsByApiKey = Map.copyOf(configuredClients);
    }

    public Optional<TenantPrincipal> resolve(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(tenantsByApiKey.get(apiKey));
    }
}
