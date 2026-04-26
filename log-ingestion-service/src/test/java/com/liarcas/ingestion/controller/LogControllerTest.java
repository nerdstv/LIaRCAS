package com.liarcas.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liarcas.models.LogEvent;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, LogEvent> kafkaTemplate;

    @Test
    void shouldGenerateIdAndTimestampAndSendToKafka() throws Exception {
        String payload = """
                {
                  "tenantId": "tenant-001",
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        MvcResult result = mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        LogEvent response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LogEvent.class
        );

        assertThat(response.getId()).isNotBlank();
        assertThat(response.getTenantId()).isEqualTo("tenant-001");
        assertThat(response.getServiceName()).isEqualTo("payment-service");
        assertThat(response.getLevel()).isEqualTo("ERROR");
        assertThat(response.getMessage()).isEqualTo("Database timeout");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getComponent()).isNull();
        assertThat(response.getEnvironment()).isNull();
        assertThat(response.getServiceVersion()).isNull();
        assertThat(response.getInstanceId()).isNull();
        assertThat(response.getTraceId()).isNull();
        assertThat(response.getExceptionType()).isNull();
        assertThat(response.getStackTraceHash()).isNull();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LogEvent> eventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        verify(kafkaTemplate).send(eq("raw-logs"), keyCaptor.capture(), eventCaptor.capture());

        LogEvent sentEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo(response.getId());
        assertThat(sentEvent.getId()).isEqualTo(response.getId());
        assertThat(sentEvent.getTenantId()).isEqualTo("tenant-001");
        assertThat(sentEvent.getTimestamp()).isEqualTo(response.getTimestamp());
        assertThat(sentEvent.getServiceName()).isEqualTo("payment-service");
        assertThat(sentEvent.getLevel()).isEqualTo("ERROR");
        assertThat(sentEvent.getMessage()).isEqualTo("Database timeout");
        assertThat(sentEvent.getComponent()).isNull();
        assertThat(sentEvent.getEnvironment()).isNull();
        assertThat(sentEvent.getServiceVersion()).isNull();
        assertThat(sentEvent.getInstanceId()).isNull();
        assertThat(sentEvent.getTraceId()).isNull();
        assertThat(sentEvent.getExceptionType()).isNull();
        assertThat(sentEvent.getStackTraceHash()).isNull();
    }

    @Test
    void shouldKeepProvidedIdTimestampTenantIdAndRcaMetadata() throws Exception {
        LogEvent request = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                Instant.parse("2026-04-21T10:15:30Z")
        );
        request.setTenantId("tenant-001");
        request.setComponent("db-client");
        request.setEnvironment("prod");
        request.setServiceVersion("1.4.2");
        request.setInstanceId("payment-pod-7");
        request.setTraceId("trace-abc-123");
        request.setExceptionType("SQLTransientConnectionException");
        request.setStackTraceHash("sth-9f8c2d");

        MvcResult result = mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LogEvent response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LogEvent.class
        );

        assertThat(response.getId()).isEqualTo("log-123");
        assertThat(response.getTenantId()).isEqualTo("tenant-001");
        assertThat(response.getTimestamp()).isEqualTo(Instant.parse("2026-04-21T10:15:30Z"));
        assertThat(response.getComponent()).isEqualTo("db-client");
        assertThat(response.getEnvironment()).isEqualTo("prod");
        assertThat(response.getServiceVersion()).isEqualTo("1.4.2");
        assertThat(response.getInstanceId()).isEqualTo("payment-pod-7");
        assertThat(response.getTraceId()).isEqualTo("trace-abc-123");
        assertThat(response.getExceptionType()).isEqualTo("SQLTransientConnectionException");
        assertThat(response.getStackTraceHash()).isEqualTo("sth-9f8c2d");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LogEvent> eventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        verify(kafkaTemplate).send(eq("raw-logs"), keyCaptor.capture(), eventCaptor.capture());

        LogEvent sentEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo("log-123");
        assertThat(sentEvent.getId()).isEqualTo("log-123");
        assertThat(sentEvent.getTenantId()).isEqualTo("tenant-001");
        assertThat(sentEvent.getTimestamp()).isEqualTo(Instant.parse("2026-04-21T10:15:30Z"));
        assertThat(sentEvent.getComponent()).isEqualTo("db-client");
        assertThat(sentEvent.getEnvironment()).isEqualTo("prod");
        assertThat(sentEvent.getServiceVersion()).isEqualTo("1.4.2");
        assertThat(sentEvent.getInstanceId()).isEqualTo("payment-pod-7");
        assertThat(sentEvent.getTraceId()).isEqualTo("trace-abc-123");
        assertThat(sentEvent.getExceptionType()).isEqualTo("SQLTransientConnectionException");
        assertThat(sentEvent.getStackTraceHash()).isEqualTo("sth-9f8c2d");
    }

    @Test
    void shouldRejectMissingTenantId() throws Exception {
        String payload = """
                {
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        mockMvc.perform(post("/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(kafkaTemplate);
    }
}