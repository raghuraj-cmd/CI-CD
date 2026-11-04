package com.experiment.cd.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Actuator Health Indicator for CD Pipeline Health Verification.
 * In a real Continuous Deployment pipeline, health indicators are polled
 * by the CD orchestrator (e.g. Kubernetes readiness probes or GitHub Actions
 * smoke tests) to determine whether the newly deployed version is healthy
 * or should be rolled back immediately.
 */
@Component
public class DeploymentHealthIndicator implements HealthIndicator {

    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private String failureReason = "";

    @Override
    public Health health() {
        if (isHealthy.get()) {
            return Health.up()
                .withDetail("deployment", "HEALTHY")
                .withDetail("readinessProbe", "PASSING")
                .withDetail("trafficStatus", "ACCEPTING_TRAFFIC")
                .build();
        } else {
            return Health.down()
                .withDetail("deployment", "DEGRADED")
                .withDetail("readinessProbe", "FAILING")
                .withDetail("failureReason", failureReason)
                .build();
        }
    }

    public void setHealthy(boolean healthy, String reason) {
        this.isHealthy.set(healthy);
        this.failureReason = reason != null ? reason : "";
    }

    public boolean isHealthy() {
        return isHealthy.get();
    }
}
