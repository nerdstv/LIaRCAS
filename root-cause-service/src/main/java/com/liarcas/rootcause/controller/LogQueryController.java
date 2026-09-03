package com.liarcas.rootcause.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.liarcas.rootcause.dto.LogEntryResponse;
import com.liarcas.rootcause.dto.LogQueryFilter;
import com.liarcas.rootcause.dto.PagedResponse;
import com.liarcas.rootcause.security.TenantPrincipal;
import com.liarcas.rootcause.service.LogQueryService;

/**
 * Exposes a read-only, tenant-scoped log query API.
 *
 * Tenant scope always comes from the authenticated caller's TenantPrincipal, never
 * from request parameters, so a caller cannot retrieve another tenant's logs by
 * supplying a different tenant identifier.
 */
@RestController
public class LogQueryController {

    private final LogQueryService logQueryService;

    public LogQueryController(LogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    /**
     * Lists the authenticated tenant's log entries, optionally filtered and paginated.
     *
     * @param serviceName optional exact service name filter
     * @param level optional exact log level filter
     * @param from optional inclusive lower bound on timestamp (ISO-8601)
     * @param to optional inclusive upper bound on timestamp (ISO-8601)
     * @param page zero-based page index, defaults to 0
     * @param size page size, defaults to 20
     * @param authentication authenticated tenant context
     * @return a page of log entries belonging to the caller's tenant
     */
    @GetMapping("/logs")
    public PagedResponse<LogEntryResponse> getLogs(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof TenantPrincipal tenantPrincipal)) {
            throw new IllegalStateException("Authenticated tenant principal is required");
        }

        LogQueryFilter filter = new LogQueryFilter(serviceName, level, from, to);
        return logQueryService.queryLogs(tenantPrincipal.tenantId(), filter, page, size);
    }
}
