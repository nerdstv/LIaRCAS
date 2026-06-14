package com.liarcas.ingestion.request;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liarcas.request")
public class RequestSizeProperties {

    /**
     * Maximum allowed request size in bytes for the /logs endpoint.
     * Default: 1 MB (1048576 bytes)
     */
    private long maxSize = 1048576;

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
