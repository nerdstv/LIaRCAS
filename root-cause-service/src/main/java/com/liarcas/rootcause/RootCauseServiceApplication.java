package com.liarcas.rootcause;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class for the root cause Spring Boot application.
 */
@SpringBootApplication
public class RootCauseServiceApplication {

    /**
     * Starts the root cause service.
     *
     * @param args runtime arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(RootCauseServiceApplication.class, args);
    }
}
