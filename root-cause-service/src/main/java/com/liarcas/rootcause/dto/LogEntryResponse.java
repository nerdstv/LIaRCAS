package com.liarcas.rootcause.dto;

import java.time.Instant;

/**
 * API response shape for a single log entry.
 *
 * @param id event identifier
 * @param serviceName originating service name
 * @param component component that emitted the log
 * @param environment deployment environment
 * @param serviceVersion service version string
 * @param instanceId runtime instance identifier
 * @param traceId distributed tracing identifier
 * @param level log severity level
 * @param message log message content
 * @param exceptionType exception class name when applicable
 * @param stackTraceHash hash of the stack trace content
 * @param timestamp event timestamp
 */
public record LogEntryResponse(
        String id,
        String serviceName,
        String component,
        String environment,
        String serviceVersion,
        String instanceId,
        String traceId,
        String level,
        String message,
        String exceptionType,
        String stackTraceHash,
        Instant timestamp
) {
}
