package com.liarcas.processing.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.liarcas.models.LogEvent;
import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.service.TenantScopedDocumentService;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LogConsumerTest {

    @Mock
    private TenantScopedDocumentService tenantScopedDocumentService;

    @InjectMocks
    private LogConsumer logConsumer;

    @Test
    void shouldMapKafkaMessageAndSaveDocumentWithTenantAndRcaMetadata() {
        Instant timestamp = Instant.parse("2026-04-21T10:15:30Z");
        LogEvent message = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                timestamp
        );
        message.setTenantId("tenant-001");
        message.setComponent("db-client");
        message.setEnvironment("prod");
        message.setServiceVersion("1.4.2");
        message.setInstanceId("payment-pod-7");
        message.setTraceId("trace-abc-123");
        message.setExceptionType("SQLTransientConnectionException");
        message.setStackTraceHash("sth-9f8c2d");

        logConsumer.consume(message);

        ArgumentCaptor<LogEventDocument> captor = ArgumentCaptor.forClass(LogEventDocument.class);
        verify(tenantScopedDocumentService).save(captor.capture());

        LogEventDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getId()).isEqualTo("log-123");
        assertThat(savedDocument.getTenantId()).isEqualTo("tenant-001");
        assertThat(savedDocument.getServiceName()).isEqualTo("payment-service");
        assertThat(savedDocument.getLevel()).isEqualTo("ERROR");
        assertThat(savedDocument.getMessage()).isEqualTo("Database timeout");
        assertThat(savedDocument.getComponent()).isEqualTo("db-client");
        assertThat(savedDocument.getEnvironment()).isEqualTo("prod");
        assertThat(savedDocument.getServiceVersion()).isEqualTo("1.4.2");
        assertThat(savedDocument.getInstanceId()).isEqualTo("payment-pod-7");
        assertThat(savedDocument.getTraceId()).isEqualTo("trace-abc-123");
        assertThat(savedDocument.getExceptionType()).isEqualTo("SQLTransientConnectionException");
        assertThat(savedDocument.getStackTraceHash()).isEqualTo("sth-9f8c2d");
        assertThat(savedDocument.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void shouldRejectAndLogWhenTenantIdIsMissing(CapturedOutput output) {
        LogEvent message = new LogEvent(
                "log-no-tenant",
                "payment-service",
                "ERROR",
                "Database timeout",
                Instant.parse("2026-04-21T10:15:30Z")
        );

        logConsumer.consume(message);

        verifyNoInteractions(tenantScopedDocumentService);
        assertThat(output.getOut())
                .contains("reason=missing_tenant_id")
                .contains("eventId=log-no-tenant")
                .contains("topic=raw-logs");
    }
}