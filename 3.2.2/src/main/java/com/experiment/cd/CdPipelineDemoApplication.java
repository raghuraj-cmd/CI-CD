package com.experiment.cd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for Continuous Deployment (CD) Demo application.
 * Demonstrates:
 *  - Production containerization
 *  - Multi-stage Docker packaging
 *  - Health, readiness, and liveness actuator endpoints
 *  - Automated staging & production deployment pipelines
 *  - Smoke testing and rollback mechanisms
 */
@SpringBootApplication
public class CdPipelineDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdPipelineDemoApplication.class, args);
    }
}
