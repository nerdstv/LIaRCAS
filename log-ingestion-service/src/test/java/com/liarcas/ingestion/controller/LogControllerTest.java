package com.liarcas.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        assertThat(response.getServiceName()).isEqualTo("payment-service");
        assertThat(response.getLevel()).isEqualTo("ERROR");
        assertThat(response.getMessage()).isEqualTo("Database timeout");
        assertThat(response.getTimestamp()).isNotNull();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LogEvent> eventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        verify(kafkaTemplate).send(eq("raw-logs"), keyCaptor.capture(), eventCaptor.capture());

        LogEvent sentEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo(response.getId());
        assertThat(sentEvent.getId()).isEqualTo(response.getId());
        assertThat(sentEvent.getTimestamp()).isEqualTo(response.getTimestamp());
        assertThat(sentEvent.getServiceName()).isEqualTo("payment-service");
        assertThat(sentEvent.getLevel()).isEqualTo("ERROR");
        assertThat(sentEvent.getMessage()).isEqualTo("Database timeout");
    }

    @Test
    void shouldKeepProvidedIdAndTimestamp() throws Exception {
        LogEvent request = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                Instant.parse("2026-04-21T10:15:30Z")
        );

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
        assertThat(response.getTimestamp()).isEqualTo(Instant.parse("2026-04-21T10:15:30Z"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LogEvent> eventCaptor = ArgumentCaptor.forClass(LogEvent.class);

        verify(kafkaTemplate).send(eq("raw-logs"), keyCaptor.capture(), eventCaptor.capture());

        LogEvent sentEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo("log-123");
        assertThat(sentEvent.getId()).isEqualTo("log-123");
        assertThat(sentEvent.getTimestamp()).isEqualTo(Instant.parse("2026-04-21T10:15:30Z"));
    }
}