package com.liarcas.rootcause.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes liveness information for the root cause service.
 */
@RestController
public class HealthController {

    /**
     * Returns a simple health payload for probes and smoke checks.
     *
     * @return service status metadata
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "root-cause-service", "status", "UP");
    }
}
