package com.liarcas.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class for the log processing Spring Boot application.
 */
@SpringBootApplication
public class LogProcessingServiceApplication {

    /**
     * Starts the log processing service.
     *
     * @param args runtime arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(LogProcessingServiceApplication.class, args);
    }
}
