package com.liarcas.rootcause.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.liarcas.rootcause.dto.LogEntryResponse;
import com.liarcas.rootcause.dto.LogQueryFilter;
import com.liarcas.rootcause.dto.PagedResponse;
import com.liarcas.rootcause.security.SecurityConfig;
import com.liarcas.rootcause.service.LogQueryService;

@WebMvcTest(LogQueryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "liarcas.auth.header-name=X-API-Key",
        "liarcas.auth.clients[0].client-id=local-dev-client",
        "liarcas.auth.clients[0].api-key=local-dev-api-key",
        "liarcas.auth.clients[0].tenant-id=tenant-001"
})
class LogQueryControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY = "local-dev-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogQueryService logQueryService;

    @Test
    void shouldRejectRequestWithoutApiKey() throws Exception {
        mockMvc.perform(get("/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/logs").header(API_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnLogsForAuthenticatedTenant() throws Exception {
        PagedResponse<LogEntryResponse> response = new PagedResponse<>(
                List.of(new LogEntryResponse(
                        "log-1", "checkout-service", null, null, null, null, null,
                        "ERROR", "boom", null, null, java.time.Instant.parse("2026-01-01T00:00:00Z"))),
                0, 20, 1, 1
        );
        when(logQueryService.queryLogs(eq("tenant-001"), any(LogQueryFilter.class), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/logs").header(API_KEY_HEADER, API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("log-1"))
                .andExpect(jsonPath("$.content[0].serviceName").value("checkout-service"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldIgnoreClientSuppliedTenantIdAndUseAuthenticatedTenant() throws Exception {
        PagedResponse<LogEntryResponse> response = new PagedResponse<>(List.of(), 0, 20, 0, 0);
        when(logQueryService.queryLogs(eq("tenant-001"), any(LogQueryFilter.class), eq(0), eq(20)))
                .thenReturn(response);

        // A caller authenticated as tenant-001 cannot request tenant-002's data by
        // supplying a tenantId query parameter - the controller never binds it.
        mockMvc.perform(get("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .param("tenantId", "tenant-002"))
                .andExpect(status().isOk());

        verify(logQueryService).queryLogs(eq("tenant-001"), any(LogQueryFilter.class), eq(0), eq(20));
    }

    @Test
    void shouldPassFiltersAndPaginationToService() throws Exception {
        PagedResponse<LogEntryResponse> response = new PagedResponse<>(List.of(), 2, 10, 0, 0);
        when(logQueryService.queryLogs(eq("tenant-001"), any(LogQueryFilter.class), eq(2), eq(10)))
                .thenReturn(response);

        mockMvc.perform(get("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .param("serviceName", "checkout-service")
                        .param("level", "ERROR")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-02T00:00:00Z")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(logQueryService).queryLogs(eq("tenant-001"), eq(new LogQueryFilter(
                "checkout-service",
                "ERROR",
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-02T00:00:00Z")
        )), eq(2), eq(10));
    }

    @Test
    void shouldReturnBadRequestForInvalidQuery() throws Exception {
        when(logQueryService.queryLogs(eq("tenant-001"), any(LogQueryFilter.class), eq(0), eq(20)))
                .thenThrow(new IllegalArgumentException("level must be one of [...]"));

        mockMvc.perform(get("/logs")
                        .header(API_KEY_HEADER, API_KEY)
                        .param("level", "not-a-level"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid log query"));
    }
}
