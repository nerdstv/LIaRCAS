package com.liarcas.ingestion.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for API key authentication.
 */
@ConfigurationProperties(prefix = "liarcas.auth")
public class ApiKeyAuthProperties {

    private String headerName = "X-API-Key";
    private List<Client> clients = new ArrayList<>();

    /**
     * Returns the request header name that carries the API key.
     *
     * @return API key header name
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * Sets the request header name that carries the API key.
     *
     * @param headerName API key header name
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * Returns configured API key clients.
     *
     * @return client configurations
     */
    public List<Client> getClients() {
        return clients;
    }

    /**
     * Sets configured API key clients.
     *
     * @param clients client configurations
     */
    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    /**
     * Represents a single API key client configuration entry.
     */
    public static class Client {

        private String clientId;
        private String apiKey;
        private String tenantId;

        /**
         * Returns the client identifier.
         *
         * @return client identifier
         */
        public String getClientId() {
            return clientId;
        }

        /**
         * Sets the client identifier.
         *
         * @param clientId client identifier
         */
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        /**
         * Returns the client's API key.
         *
         * @return API key
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * Sets the client's API key.
         *
         * @param apiKey API key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * Returns the tenant identifier associated with the client.
         *
         * @return tenant identifier
         */
        public String getTenantId() {
            return tenantId;
        }

        /**
         * Sets the tenant identifier associated with the client.
         *
         * @param tenantId tenant identifier
         */
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
