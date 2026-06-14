package com.liarcas.ingestion.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.liarcas.ingestion.request.RequestConfig;
import com.liarcas.models.LogEvent;

/**
 * Integration tests for request size limiting behavior.
 * Tests that the RequestSizeFilter properly rejects oversized requests
 * and returns 413 Payload Too Large before Kafka publication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(RequestConfig.class)
@TestPropertySource(properties = {
        "liarcas.auth.header-name=X-API-Key",
        "liarcas.auth.clients[0].client-id=local-dev-client",
        "liarcas.auth.clients[0].api-key=local-dev-api-key",
        "liarcas.auth.clients[0].tenant-id=tenant-001",
        "liarcas.request.max-size=500"  // Set small limit for testing
})
class LogControllerRequestSizeIT {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY = "local-dev-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaTemplate<String, LogEvent> kafkaTemplate;

    @Test
    void shouldRejectRequestExceedingMaxSize() throws Exception {
        // Create a payload that exceeds 500 bytes
        String largePayload = """
                {
                  "serviceName": "payment-service",
                  "component": "payment-processor",
                  "environment": "production",
                  "serviceVersion": "2.1.0",
                  "instanceId": "payment-pod-123-abc",
                  "traceId": "trace-correlate-abc-123-xyz-987",
                  "level": "ERROR",
                  "message": "Database timeout occurred while processing the payment request. This is a very long error message that contains detailed information about what went wrong. The database failed to respond within the timeout period, which is typically 30 seconds for read operations and 60 seconds for write operations. We have implemented retry logic with exponential backoff, but after 3 attempts the operation ultimately failed. The error occurred at approximately 2026-06-14T10:30:45Z. Service version: 2.1.0, Build: 12345, Deployment: production-us-east-1",
                  "exceptionType": "java.sql.SQLTransientConnectionException",
                  "stackTraceHash": "abc123def456xyz789"
                }
                """;

        byte[] payload = largePayload.getBytes();
        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .header("Content-Length", String.valueOf(payload.length))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isPayloadTooLarge())  // HTTP 413
                .andExpect(jsonPath("$.type").value("urn:liarcas:problem:payload-too-large"))
                .andExpect(jsonPath("$.title").value("Payload Too Large"))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/logs"));

        // Verify Kafka was never invoked
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldAcceptNormalSizedRequest() throws Exception {
        String normalPayload = """
                {
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        byte[] payload = normalPayload.getBytes();
        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .header("Content-Length", String.valueOf(payload.length))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(normalPayload))
                .andExpect(status().isOk());

        // Kafka should be invoked for normal-sized requests
        // We verify this indirectly via successful status
    }
}
