package com.experiment.cd.controller;

import com.experiment.cd.health.DeploymentHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller providing Deployment inspection, smoke testing,
 * and failure simulation endpoints for the CD pipeline.
 */
@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentHealthIndicator healthIndicator;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.environment:local}")
    private String appEnvironment;

    @Value("${app.deployment.type:STABLE}")
    private String deploymentType;

    /**
     * GET /api/deployments/info
     * Returns application version, deployment environment, and uptime.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDeploymentInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "cd-pipeline-demo");
        info.put("version", appVersion);
        info.put("environment", appEnvironment);
        info.put("deploymentType", deploymentType);
        info.put("status", healthIndicator.isHealthy() ? "HEALTHY" : "DEGRADED");
        info.put("uptimeMillis", ManagementFactory.getRuntimeMXBean().getUptime());
        info.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(info);
    }

    /**
     * GET /api/deployments/smoke-test
     * Smoke-test endpoint called by the CD deployment pipeline
     * to verify application readiness immediately post-deployment.
     */
    @GetMapping("/smoke-test")
    public ResponseEntity<Map<String, String>> smokeTest() {
        if (!healthIndicator.isHealthy()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("smokeTest", "FAILED", "reason", "Readiness probe is degraded"));
        }
        return ResponseEntity.ok(Map.of(
            "smokeTest", "PASSED",
            "version", appVersion,
            "environment", appEnvironment,
            "status", "READY"
        ));
    }

    /**
     * POST /api/deployments/simulate-failure
     * Simulates deployment failure to trigger automated rollback in CD pipeline.
     */
    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, String>> simulateFailure(
            @RequestParam(defaultValue = "Simulated synthetic outage during smoke test") String reason) {
        healthIndicator.setHealthy(false, reason);
        return ResponseEntity.ok(Map.of("status", "FAILURE_SIMULATED", "reason", reason));
    }

    /**
     * POST /api/deployments/recover
     * Recovers deployment health to normal.
     */
    @PostMapping("/recover")
    public ResponseEntity<Map<String, String>> recover() {
        healthIndicator.setHealthy(true, null);
        return ResponseEntity.ok(Map.of("status", "RECOVERED"));
    }
}
