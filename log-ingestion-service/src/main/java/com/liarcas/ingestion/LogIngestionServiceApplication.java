package com.liarcas.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class for the log ingestion Spring Boot application.
 */
@SpringBootApplication
public class LogIngestionServiceApplication {

    /**
     * Starts the log ingestion service.
     *
     * @param args runtime arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(LogIngestionServiceApplication.class, args);
    }
}
