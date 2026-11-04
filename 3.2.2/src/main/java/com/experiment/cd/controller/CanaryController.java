package com.experiment.cd.controller;

import com.experiment.cd.model.CanaryStatus;
import com.experiment.cd.service.CanaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller exposing Canary Deployment inspection,
 * weighted routing simulation, and automated promotion/rollback controls.
 */
@RestController
@RequestMapping("/api/canary")
@RequiredArgsConstructor
public class CanaryController {

    private final CanaryService canaryService;

    /**
     * GET /api/canary/status
     * Returns real-time status of canary deployment, traffic weights, and error metrics.
     */
    @GetMapping("/status")
    public ResponseEntity<CanaryStatus> getStatus() {
        return ResponseEntity.ok(canaryService.getStatus());
    }

    /**
     * GET /api/canary/route
     * Evaluates incoming request routing.
     * Respects 'X-Canary' header if provided (e.g. 'always', 'true', 'false');
     * otherwise applies configured weighted split.
     */
    @GetMapping("/route")
    public ResponseEntity<Map<String, Object>> routeRequest(
            @RequestHeader(value = "X-Canary", required = false) String canaryHeader) {
        Map<String, Object> response = canaryService.routeRequest(canaryHeader);
        int statusCode = (int) response.getOrDefault("statusCode", 200);
        return ResponseEntity.status(statusCode).body(response);
    }

    /**
     * POST /api/canary/simulate-traffic
     * Simulates batch requests (default 20) to demonstrate traffic distribution.
     */
    @PostMapping("/simulate-traffic")
    public ResponseEntity<Map<String, Object>> simulateTraffic(
            @RequestParam(defaultValue = "20") int count) {
        return ResponseEntity.ok(canaryService.simulateTraffic(count));
    }

    /**
     * POST /api/canary/set-weight
     * Updates canary traffic weight percentage (e.g. 10%, 25%, 50%, 100%).
     */
    @PostMapping("/set-weight")
    public ResponseEntity<CanaryStatus> setWeight(@RequestParam int weight) {
        return ResponseEntity.ok(canaryService.setCanaryWeight(weight));
    }

    /**
     * POST /api/canary/promote
     * Promotes canary release to 100% production stable.
     */
    @PostMapping("/promote")
    public ResponseEntity<CanaryStatus> promote() {
        return ResponseEntity.ok(canaryService.promoteCanary());
    }

    /**
     * POST /api/canary/rollback
     * Immediately aborts canary release and reroutes all traffic to stable.
     */
    @PostMapping("/rollback")
    public ResponseEntity<CanaryStatus> rollback(
            @RequestParam(defaultValue = "Canary error rate exceeded 5% threshold") String reason) {
        return ResponseEntity.ok(canaryService.rollbackCanary(reason));
    }

    /**
     * POST /api/canary/simulate-failure
     * Injects synthetic error into canary instance to demonstrate automated rollback trigger.
     */
    @PostMapping("/simulate-failure")
    public ResponseEntity<Map<String, String>> simulateFailure(
            @RequestParam(defaultValue = "Synthetic canary regression / 500 error") String reason) {
        canaryService.setCanaryHealth(false, reason);
        return ResponseEntity.ok(Map.of("status", "CANARY_FAILURE_INJECTED", "reason", reason));
    }

    /**
     * POST /api/canary/recover
     * Recovers canary health to normal.
     */
    @PostMapping("/recover")
    public ResponseEntity<Map<String, String>> recover() {
        canaryService.setCanaryHealth(true, null);
        return ResponseEntity.ok(Map.of("status", "CANARY_RECOVERED"));
    }

    /**
     * POST /api/canary/reset
     * Resets counters and sets weights back to initial 10% canary state.
     */
    @PostMapping("/reset")
    public ResponseEntity<CanaryStatus> reset() {
        canaryService.resetMetrics();
        return ResponseEntity.ok(canaryService.getStatus());
    }
}

