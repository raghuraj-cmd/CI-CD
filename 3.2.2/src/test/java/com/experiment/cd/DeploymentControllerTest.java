package com.experiment.cd;

import com.experiment.cd.health.DeploymentHealthIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUTOMATED SMOKE & HEALTH TESTS FOR CD PIPELINE
 * ================================================
 * Validates the deployment controller, actuator health probes,
 * and automated failure detection for CD rollbacks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CD Pipeline Deployment & Health Verification Tests")
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator.setHealthy(true, null);
    }

    @Test
    @DisplayName("GET /api/deployments/info - Should return release metadata and HEALTHY status")
    void testGetDeploymentInfo() throws Exception {
        mockMvc.perform(get("/api/deployments/info")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service", is("cd-pipeline-demo")))
            .andExpect(jsonPath("$.version", is("1.0.0")))
            .andExpect(jsonPath("$.status", is("HEALTHY")));
    }

    @Test
    @DisplayName("GET /api/deployments/smoke-test - Should return 200 OK when healthy")
    void testSmokeTestSuccess() throws Exception {
        mockMvc.perform(get("/api/deployments/smoke-test"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.smokeTest", is("PASSED")))
            .andExpect(jsonPath("$.status", is("READY")));
    }

    @Test
    @DisplayName("Simulate Failure -> Smoke Test should return 503 SERVICE_UNAVAILABLE (Rollback trigger)")
    void testSmokeTestFailureAndRollbackTrigger() throws Exception {
        // 1. Simulate failure
        mockMvc.perform(post("/api/deployments/simulate-failure")
                .param("reason", "Database connectivity failure"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("FAILURE_SIMULATED")));

        // 2. Smoke test now fails with 503
        mockMvc.perform(get("/api/deployments/smoke-test"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.smokeTest", is("FAILED")));

        // 3. Actuator health endpoint reflects DOWN status
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status", is("DOWN")));

        // 4. Recover
        mockMvc.perform(post("/api/deployments/recover"))
            .andExpect(status().isOk());

        // 5. Smoke test passes again
        mockMvc.perform(get("/api/deployments/smoke-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.smokeTest", is("PASSED")));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness - Should report UP for Kubernetes / CD readiness probe")
    void testReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/liveness - Should report UP for liveness probe")
    void testLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }
}
