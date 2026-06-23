package com.liarcas.ingestion.request;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for request size limits in ingestion endpoints.
 */
@ConfigurationProperties(prefix = "liarcas.request")
public class RequestSizeProperties {

    /**
     * Maximum allowed request size in bytes for the /logs endpoint.
     * Default: 1 MB (1048576 bytes)
     */
    private long maxSize = 1048576;

    /**
     * Returns the maximum allowed request size in bytes.
     *
     * @return max request size in bytes
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum allowed request size in bytes.
     *
     * @param maxSize max request size in bytes
     */
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
