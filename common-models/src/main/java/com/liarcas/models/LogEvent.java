package com.liarcas.models;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents a normalized log event exchanged between ingestion and processing services.
 */
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

    /**
     * Creates an empty log event.
     */
    public LogEvent() {
    }

    /**
     * Creates a minimal log event.
     *
     * @param id event identifier
     * @param serviceName originating service name
     * @param level log severity level
     * @param message log message content
     * @param timestamp event timestamp
     */
    public LogEvent(String id, String serviceName, String level, String message, Instant timestamp) {
        this.id = id;
        this.serviceName = serviceName;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * Creates a fully populated log event.
     *
     * @param id event identifier
     * @param tenantId tenant identifier
     * @param serviceName originating service name
     * @param component component that emitted the log
     * @param environment deployment environment
     * @param serviceVersion service version string
     * @param instanceId runtime instance identifier
     * @param traceId distributed tracing identifier
     * @param level log severity level
     * @param message log message content
     * @param exceptionType exception class name when applicable
     * @param stackTraceHash hash of the stack trace content
     * @param timestamp event timestamp
     */
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

    /**
     * Returns the tenant identifier.
     *
     * @return tenant identifier
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant identifier.
     *
     * @param tenantId tenant identifier
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Returns the event identifier.
     *
     * @return event identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the event identifier.
     *
     * @param id event identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the originating service name.
     *
     * @return service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the originating service name.
     *
     * @param serviceName service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Returns the component name.
     *
     * @return component name
     */
    public String getComponent() {
        return component;
    }

    /**
     * Sets the component name.
     *
     * @param component component name
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * Returns the environment.
     *
     * @return environment name
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Sets the environment.
     *
     * @param environment environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Returns the service version.
     *
     * @return service version
     */
    public String getServiceVersion() {
        return serviceVersion;
    }

    /**
     * Sets the service version.
     *
     * @param serviceVersion service version
     */
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    /**
     * Returns the instance identifier.
     *
     * @return instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the instance identifier.
     *
     * @param instanceId instance identifier
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the distributed tracing identifier.
     *
     * @return trace identifier
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Sets the distributed tracing identifier.
     *
     * @param traceId trace identifier
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * Returns the log severity.
     *
     * @return log level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the log severity.
     *
     * @param level log level
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Returns the log message content.
     *
     * @return log message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the log message content.
     *
     * @param message log message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the exception type if present.
     *
     * @return exception type
     */
    public String getExceptionType() {
        return exceptionType;
    }

    /**
     * Sets the exception type.
     *
     * @param exceptionType exception type
     */
    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    /**
     * Returns the stack trace hash if present.
     *
     * @return stack trace hash
     */
    public String getStackTraceHash() {
        return stackTraceHash;
    }

    /**
     * Sets the stack trace hash.
     *
     * @param stackTraceHash stack trace hash
     */
    public void setStackTraceHash(String stackTraceHash) {
        this.stackTraceHash = stackTraceHash;
    }

    /**
     * Returns the event timestamp.
     *
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the event timestamp.
     *
     * @param timestamp timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a printable representation of this log event.
     *
     * @return string representation
     */
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
