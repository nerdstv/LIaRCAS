package com.liarcas.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.liarcas.ingestion.security.SecurityConfig;
import com.liarcas.models.LogEvent;

@WebMvcTest(LogController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "liarcas.auth.header-name=X-API-Key",
        "liarcas.auth.clients[0].client-id=local-dev-client",
        "liarcas.auth.clients[0].api-key=local-dev-api-key",
        "liarcas.auth.clients[0].tenant-id=tenant-001"
})
class LogControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY = "local-dev-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, LogEvent> kafkaTemplate;

    @BeforeEach
    void clearKafkaTemplateInteractions() {
        clearInvocations(kafkaTemplate);
    }

        @Test
        void shouldRejectOversizedValues() throws Exception {
                String longServiceName = "a".repeat(101);
                String longComponent = "b".repeat(101);
                String longEnvironment = "c".repeat(51);
                String longServiceVersion = "d".repeat(51);
                String longInstanceId = "e".repeat(101);
                String longTraceId = "f".repeat(129);
                String longLevel = "ERROR"; // valid level
                String longMessage = "m".repeat(10001);
                String longExceptionType = "x".repeat(201);
                String longStackTraceHash = "y".repeat(129);

                com.liarcas.models.LogEvent request = new com.liarcas.models.LogEvent(
                                "log-oversize",
                                null,
                                longServiceName,
                                longComponent,
                                longEnvironment,
                                longServiceVersion,
                                longInstanceId,
                                longTraceId,
                                longLevel,
                                longMessage,
                                longExceptionType,
                                longStackTraceHash,
                                Instant.now()
                );

                MvcResult result = mockMvc.perform(post("/logs")
                                                .header(API_KEY_HEADER, API_KEY)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                JsonNode errors = root.get("errors");

                String[][] expected = new String[][]{
                                {"serviceName", "serviceName must not exceed 100 characters"},
                                {"component", "component must not exceed 100 characters"},
                                {"environment", "environment must not exceed 50 characters"},
                                {"serviceVersion", "serviceVersion must not exceed 50 characters"},
                                {"instanceId", "instanceId must not exceed 100 characters"},
                                {"traceId", "traceId must not exceed 128 characters"},
                                {"message", "message must not exceed 10000 characters"},
                                {"exceptionType", "exceptionType must not exceed 200 characters"},
                                {"stackTraceHash", "stackTraceHash must not exceed 128 characters"}
                };

                for (String[] pair : expected) {
                        String field = pair[0];
                        String msg = pair[1];
                        boolean found = false;
                        for (JsonNode err : errors) {
                                if (err.has("field") && err.has("message")
                                                && field.equals(err.get("field").asText())
                                                && msg.equals(err.get("message").asText())) {
                                        found = true;
                                        break;
                                }
                        }
                        assertThat(found).withFailMessage("Expected validation error for %s: %s", field, msg).isTrue();
                }

                verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldRejectInvalidLevelValue() throws Exception {
                String payload = """
                                {
                                  "serviceName": "payment-service",
                                  "level": "INVALID",
                                  "message": "Database timeout"
                                }
                                """;

                MvcResult result = mockMvc.perform(post("/logs")
                                                .header(API_KEY_HEADER, API_KEY)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andReturn();

                JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                JsonNode errors = root.get("errors");

                boolean found = false;
                for (JsonNode err : errors) {
                        if (err.has("field") && "level".equals(err.get("field").asText())) {
                                if (err.has("message") && "level must be a valid log level".equals(err.get("message").asText())) {
                                        found = true;
                                        break;
                                }
                        }
                }

                assertThat(found).isTrue();
                verifyNoInteractions(kafkaTemplate);
        }

    @Test
    void shouldGenerateIdAndTimestampAndResolveTenantFromApiKey() throws Exception {
        String payload = """
                {
                  "tenantId": "spoofed-tenant",
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        MvcResult result = mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
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
    void shouldKeepProvidedIdTimestampAndOverrideSpoofedTenantId() throws Exception {
        LogEvent request = new LogEvent(
                "log-123",
                "payment-service",
                "ERROR",
                "Database timeout",
                Instant.parse("2026-04-21T10:15:30Z")
        );
        request.setTenantId("spoofed-tenant");
        request.setComponent("db-client");
        request.setEnvironment("prod");
        request.setServiceVersion("1.4.2");
        request.setInstanceId("payment-pod-7");
        request.setTraceId("trace-abc-123");
        request.setExceptionType("SQLTransientConnectionException");
        request.setStackTraceHash("sth-9f8c2d");

        MvcResult result = mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
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
    void shouldRejectMissingApiKey() throws Exception {
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
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectInvalidApiKey() throws Exception {
        String payload = """
                {
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectMissingServiceName() throws Exception {
        String payload = """
                {
                  "level": "ERROR",
                  "message": "Database timeout"
                }
                """;

        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:liarcas:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("One or more request fields are invalid."))
                .andExpect(jsonPath("$.instance").value("/logs"))
                .andExpect(jsonPath("$.errors[0].field").value("serviceName"))
                .andExpect(jsonPath("$.errors[0].message").value("serviceName is required"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectBlankLevel() throws Exception {
        String payload = """
                {
                  "serviceName": "payment-service",
                  "level": "   ",
                  "message": "Database timeout"
                }
                """;

        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:liarcas:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("One or more request fields are invalid."))
                .andExpect(jsonPath("$.instance").value("/logs"))
                .andExpect(jsonPath("$.errors[0].field").value("level"))
                .andExpect(jsonPath("$.errors[0].message").value("level is required"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectMissingMessage() throws Exception {
        String payload = """
                {
                  "serviceName": "payment-service",
                  "level": "ERROR"
                }
                """;

        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:liarcas:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("One or more request fields are invalid."))
                .andExpect(jsonPath("$.instance").value("/logs"))
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].message").value("message is required"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldReturnProblemDetailForMalformedJson() throws Exception {
        String payload = """
                {
                  "serviceName": "payment-service",
                  "level": "ERROR",
                  "message":
                }
                """;

        mockMvc.perform(post("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:liarcas:problem:malformed-json"))
                .andExpect(jsonPath("$.title").value("Malformed JSON request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request body must contain valid JSON."))
                .andExpect(jsonPath("$.instance").value("/logs"))
                .andExpect(jsonPath("$.errors[0].field").value("body"))
                .andExpect(jsonPath("$.errors[0].message").value("Request body must contain valid JSON."));

        verifyNoInteractions(kafkaTemplate);
    }
}

