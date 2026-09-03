package com.liarcas.rootcause.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Read model for a log event document stored in Elasticsearch.
 *
 * Documents are read from tenant-scoped indices named liarcas-logs-{tenantId}.
 * The actual index name is resolved at query time by IndexNameUtil, using the
 * tenant derived from the caller's authentication - never from client input.
 *
 * Field mapping mirrors log-processing-service's LogEventDocument, since both
 * services must agree on how documents are stored and read.
 */
@Document(indexName = "logs-placeholder")
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

    /** Required by Spring Data for entity instantiation on read. */
    public LogEventDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
}
