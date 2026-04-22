package com.liarcas.processing.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "logs")
public class LogEventDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Keyword)
    private String serviceName;

    @Field(type = FieldType.Keyword)
    private String component;

    @Field(type = FieldType.Keyword)
    private String environment;

    @Field(type = FieldType.Keyword)
    private String serviceVersion;

    @Field(type = FieldType.Keyword)
    private String instanceId;

    @Field(type = FieldType.Keyword)
    private String traceId;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private String exceptionType;

    @Field(type = FieldType.Keyword)
    private String stackTraceHash;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant timestamp;

    public LogEventDocument() {
    }

    public LogEventDocument(String id, String serviceName, String level, String message, Instant timestamp) {
        this.id = id;
        this.serviceName = serviceName;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public LogEventDocument(
            String id,
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

    public LogEventDocument(
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
        return "LogEventDocument{" +
                "id='" + id + '\'' +
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