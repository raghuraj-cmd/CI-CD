package com.experiment.ci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application entry point for the CI Pipeline Demo.
 * This application demonstrates:
 *  - Unit Testing with JUnit 5 + Mockito
 *  - Integration Testing with MockMvc
 *  - Contract Testing with Pact
 *  - Infrastructure Testing with Testcontainers
 */
@SpringBootApplication
public class CiPipelineDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CiPipelineDemoApplication.class, args);
    }
}
