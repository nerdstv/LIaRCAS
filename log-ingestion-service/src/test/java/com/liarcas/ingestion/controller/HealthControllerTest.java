package com.liarcas.ingestion.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.liarcas.ingestion.security.SecurityConfig;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "liarcas.auth.header-name=X-API-Key",
        "liarcas.auth.clients[0].client-id=local-dev-client",
        "liarcas.auth.clients[0].api-key=local-dev-api-key",
        "liarcas.auth.clients[0].tenant-id=tenant-001"
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("log-ingestion-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}