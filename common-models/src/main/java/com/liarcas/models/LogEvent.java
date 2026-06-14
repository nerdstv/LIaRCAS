package com.liarcas.models;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LogEvent {

    private String id;
    private String tenantId;

    @NotBlank(message = "serviceName is required")
    @Size(max = 100, message = "serviceName must not exceed 100 characters")
    private String serviceName;

    @Size(max = 100, message = "component must not exceed 100 characters")
    private String component;

    @Size(max = 50, message = "environment must not exceed 50 characters")
    private String environment;

    @Size(max = 50, message = "serviceVersion must not exceed 50 characters")
    private String serviceVersion;

    @Size(max = 100, message = "instanceId must not exceed 100 characters")
    private String instanceId;

    @Size(max = 128, message = "traceId must not exceed 128 characters")
    private String traceId;

    @NotBlank(message = "level is required")
    @Size(max = 10, message = "level must not exceed 10 characters")
    @Pattern(regexp = "(?i)^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)$", message = "level must be a valid log level")
    private String level;

    @NotBlank(message = "message is required")
    @Size(max = 10000, message = "message must not exceed 10000 characters")
    private String message;

    @Size(max = 200, message = "exceptionType must not exceed 200 characters")
    private String exceptionType;

    @Size(max = 128, message = "stackTraceHash must not exceed 128 characters")
    private String stackTraceHash;

    private Instant timestamp;

    public LogEvent() {
    }

    public LogEvent(String id, String serviceName, String level, String message, Instant timestamp) {
        this.id = id;
        this.serviceName = serviceName;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }
    public LogEvent(
            String id,
            String tenantId,
            String serviceName,
            String component,
            String environment,
            String serviceVersion,
            String instanceId,
            String traceId,
            String level,
            String message,
            String exceptionType,
            String stackTraceHash,
            Instant timestamp
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.serviceName = serviceName;
        this.component = component;
        this.environment = environment;
        this.serviceVersion = serviceVersion;
        this.instanceId = instanceId;
        this.traceId = traceId;
        this.level = level;
        this.message = message;
        this.exceptionType = exceptionType;
        this.stackTraceHash = stackTraceHash;
        this.timestamp = timestamp;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getStackTraceHash() {
        return stackTraceHash;
    }

    public void setStackTraceHash(String stackTraceHash) {
        this.stackTraceHash = stackTraceHash;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", component='" + component + '\'' +
                ", environment='" + environment + '\'' +
                ", serviceVersion='" + serviceVersion + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", traceId='" + traceId + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", exceptionType='" + exceptionType + '\'' +
                ", stackTraceHash='" + stackTraceHash + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
