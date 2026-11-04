package com.experiment.cd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Model representing the real-time state, traffic weights,
 * and health metrics of a Canary Deployment release.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanaryStatus {

    private String deploymentType;         // "STABLE" or "CANARY"
    private String stableVersion;          // e.g. "1.0.0"
    private String canaryVersion;          // e.g. "2.0.0-canary"
    private int canaryTrafficWeight;       // Percentage (e.g. 10, 25, 50, 100)
    private int stableTrafficWeight;       // 100 - canaryTrafficWeight
    private long totalRequests;
    private long stableRequests;
    private long canaryRequests;
    private long failedCanaryRequests;
    private double canaryErrorRate;        // Percentage
    private String canaryHealth;           // "HEALTHY" or "DEGRADED"
    private String releaseState;           // "ACTIVE_CANARY", "PROMOTED_TO_STABLE", "ROLLED_BACK"
    private String lastUpdated;
}

