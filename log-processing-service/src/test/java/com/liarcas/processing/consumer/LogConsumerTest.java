package com.liarcas.processing.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.liarcas.models.LogEvent;
import com.liarcas.processing.document.LogEventDocument;
import com.liarcas.processing.repository.LogEventRepository;

@ExtendWith(MockitoExtension.class)
class LogConsumerTest {

    @Mock
    private LogEventRepository logEventRepository;

    @InjectMocks
    private LogConsumer logConsumer;

    @Test
    void shouldMapKafkaMessageAndSaveDocument() {
        Instant timestamp = Instant.parse("2026-04-21T10:15:30Z");
        LogEvent message = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                timestamp
        );

        logConsumer.consume(message);

        ArgumentCaptor<LogEventDocument> captor = ArgumentCaptor.forClass(LogEventDocument.class);
        verify(logEventRepository).save(captor.capture());

        LogEventDocument savedDocument = captor.getValue();
        assertThat(savedDocument.getId()).isEqualTo("log-123");
        assertThat(savedDocument.getServiceName()).isEqualTo("payment-service");
        assertThat(savedDocument.getLevel()).isEqualTo("ERROR");
        assertThat(savedDocument.getMessage()).isEqualTo("Database timeout");
        assertThat(savedDocument.getTimestamp()).isEqualTo(timestamp);
    }
}