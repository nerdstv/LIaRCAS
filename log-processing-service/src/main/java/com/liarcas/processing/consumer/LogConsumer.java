package com.liarcas.processing.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.liarcas.models.LogEvent;
import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.service.TenantScopedDocumentService;

/**
 * Consumes raw log events from Kafka and persists them to Elasticsearch.
 */
@Component
public class LogConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogConsumer.class);

    private final TenantScopedDocumentService tenantScopedDocumentService;

    /**
     * Creates a Kafka consumer for log events.
     *
     * @param logEventRepository repository used to persist log documents
     */
    public LogConsumer(TenantScopedDocumentService tenantScopedDocumentService) {
        this.tenantScopedDocumentService = tenantScopedDocumentService;
    }

    /**
     * Receives a log event from Kafka and stores the mapped document.
     *
     * @param message consumed log event
     */
    @KafkaListener(topics = "raw-logs")
    public void consume(LogEvent message) {
        if (message == null) {
            LOGGER.warn("Rejected log event before persistence: reason=null_message topic=raw-logs");
            return;
        }

        if (message.getTenantId() == null || message.getTenantId().isBlank()) {
            LOGGER.warn(
                    "Rejected log event before persistence: reason=missing_tenant_id topic=raw-logs eventId={} serviceName={} traceId={} tenantId={}",
                    message.getId(),
                    message.getServiceName(),
                    message.getTraceId(),
                    message.getTenantId()
            );
            return;
        }

        LogEventDocument document = new LogEventDocument(
                message.getId(),
                message.getTenantId(),
                message.getServiceName(),
                message.getComponent(),
                message.getEnvironment(),
                message.getServiceVersion(),
                message.getInstanceId(),
                message.getTraceId(),
                message.getLevel(),
                message.getMessage(),
                message.getExceptionType(),
                message.getStackTraceHash(),
                message.getTimestamp()
        );

        tenantScopedDocumentService.save(document);

        LOGGER.info("Consumed and saved log event: {} for tenant: {}", document.getId(), document.getTenantId());
    }
}
