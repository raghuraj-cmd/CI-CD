package com.experiment.cd.service;

import com.experiment.cd.model.CanaryStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service orchestrating Canary Deployment strategies:
 * - Weighted traffic splitting (e.g. 10% canary, 90% stable)
 * - Header-based routing overrides (e.g. X-Canary: always)
 * - Real-time error budget & metric monitoring
 * - Automated Canary Promotion and Rollback triggers
 */
@Service
public class CanaryService {

    @Value("${app.version:1.0.0}")
    private String defaultStableVersion;

    private String stableVersion = "1.0.0";
    private String canaryVersion = "2.0.0-canary";
    private int canaryWeight = 10; // Default 10% canary traffic
    private String releaseState = "ACTIVE_CANARY";

    private final AtomicBoolean canaryHealthy = new AtomicBoolean(true);
    private String canaryFailureReason = "";

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong stableRequests = new AtomicLong(0);
    private final AtomicLong canaryRequests = new AtomicLong(0);
    private final AtomicLong failedCanaryRequests = new AtomicLong(0);

    public CanaryStatus getStatus() {
        long total = totalRequests.get();
        long cReq = canaryRequests.get();
        long failed = failedCanaryRequests.get();
        double errorRate = (cReq > 0) ? ((double) failed / cReq) * 100.0 : 0.0;

        return CanaryStatus.builder()
            .deploymentType("CANARY_RELEASE_MANAGER")
            .stableVersion(stableVersion)
            .canaryVersion(canaryVersion)
            .canaryTrafficWeight(canaryWeight)
            .stableTrafficWeight(100 - canaryWeight)
            .totalRequests(total)
            .stableRequests(stableRequests.get())
            .canaryRequests(cReq)
            .failedCanaryRequests(failed)
            .canaryErrorRate(Math.round(errorRate * 10.0) / 10.0)
            .canaryHealth(canaryHealthy.get() ? "HEALTHY" : "DEGRADED")
            .releaseState(releaseState)
            .lastUpdated(Instant.now().toString())
            .build();
    }

    /**
     * Determines whether an incoming request routes to CANARY or STABLE.
     * Evaluates X-Canary header first, followed by weighted percentage.
     */
    public Map<String, Object> routeRequest(String canaryHeader) {
        totalRequests.incrementAndGet();
        boolean routeToCanary;
        String routingReason;

        if ("always".equalsIgnoreCase(canaryHeader) || "true".equalsIgnoreCase(canaryHeader)) {
            routeToCanary = true;
            routingReason = "Header-based routing (X-Canary: " + canaryHeader + ")";
        } else if ("never".equalsIgnoreCase(canaryHeader) || "false".equalsIgnoreCase(canaryHeader)) {
            routeToCanary = false;
            routingReason = "Header-based routing override (X-Canary: false)";
        } else {
            int roll = ThreadLocalRandom.current().nextInt(100);
            routeToCanary = roll < canaryWeight;
            routingReason = "Weighted traffic routing (" + canaryWeight + "% canary)";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (routeToCanary) {
            canaryRequests.incrementAndGet();
            boolean healthy = canaryHealthy.get();
            if (!healthy) {
                failedCanaryRequests.incrementAndGet();
            }
            result.put("servedBy", "CANARY");
            result.put("version", canaryVersion);
            result.put("status", healthy ? "SUCCESS" : "ERROR");
            result.put("statusCode", healthy ? 200 : 500);
            result.put("routingReason", routingReason);
            result.put("health", healthy ? "HEALTHY" : "DEGRADED: " + canaryFailureReason);
        } else {
            stableRequests.incrementAndGet();
            result.put("servedBy", "STABLE");
            result.put("version", stableVersion);
            result.put("status", "SUCCESS");
            result.put("statusCode", 200);
            result.put("routingReason", routingReason);
            result.put("health", "HEALTHY");
        }
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    /**
     * Simulates batch user traffic (e.g. N requests) to demonstrate distribution.
     */
    public Map<String, Object> simulateTraffic(int requestCount) {
        int count = Math.max(1, Math.min(requestCount, 100));
        List<Map<String, Object>> log = new ArrayList<>();
        int stableCount = 0;
        int canaryCount = 0;

        for (int i = 0; i < count; i++) {
            Map<String, Object> response = routeRequest(null);
            if ("CANARY".equals(response.get("servedBy"))) {
                canaryCount++;
            } else {
                stableCount++;
            }
            log.add(response);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("simulatedRequests", count);
        summary.put("configuredCanaryWeight", canaryWeight + "%");
        summary.put("routedToCanary", canaryCount);
        summary.put("routedToStable", stableCount);
        summary.put("canaryPercentageAchieved", Math.round(((double) canaryCount / count) * 1000.0) / 10.0 + "%");
        summary.put("recentLogs", log);
        return summary;
    }

    /**
     * Dynamically adjusts canary traffic percentage (e.g. 10% -> 25% -> 50% -> 100%).
     */
    public CanaryStatus setCanaryWeight(int percentage) {
        this.canaryWeight = Math.max(0, Math.min(percentage, 100));
        if (this.canaryWeight == 100) {
            this.releaseState = "FULL_CANARY_TEST";
        } else if (this.canaryWeight == 0) {
            this.releaseState = "CANARY_PAUSED";
        } else {
            this.releaseState = "ACTIVE_CANARY";
        }
        return getStatus();
    }

    /**
     * Promotes Canary to 100% production stable release.
     */
    public CanaryStatus promoteCanary() {
        this.stableVersion = this.canaryVersion;
        this.canaryWeight = 0;
        this.releaseState = "PROMOTED_TO_STABLE";
        this.canaryHealthy.set(true);
        this.canaryFailureReason = "";
        return getStatus();
    }

    /**
     * Rolls back Canary deployment immediately to stable version.
     */
    public CanaryStatus rollbackCanary(String reason) {
        this.canaryWeight = 0;
        this.releaseState = "ROLLED_BACK";
        this.canaryFailureReason = (reason != null && !reason.isBlank()) ? reason : "Canary error threshold breached";
        return getStatus();
    }

    /**
     * Simulates failure on Canary instance to demonstrate automated rollback trigger.
     */
    public void setCanaryHealth(boolean healthy, String reason) {
        this.canaryHealthy.set(healthy);
        this.canaryFailureReason = healthy ? "" : (reason != null ? reason : "Synthetic canary fault injected");
    }

    public boolean isCanaryHealthy() {
        return canaryHealthy.get();
    }

    public void resetMetrics() {
        totalRequests.set(0);
        stableRequests.set(0);
        canaryRequests.set(0);
        failedCanaryRequests.set(0);
        canaryWeight = 10;
        canaryHealthy.set(true);
        canaryFailureReason = "";
        releaseState = "ACTIVE_CANARY";
        stableVersion = (defaultStableVersion != null && !defaultStableVersion.isBlank()) ? defaultStableVersion : "1.0.0";
    }
}
