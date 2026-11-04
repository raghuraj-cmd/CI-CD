package com.experiment.cd;

import com.experiment.cd.service.CanaryService;
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
 * AUTOMATED CANARY DEPLOYMENT & RELEASE STRATEGY TESTS
 * ====================================================
 * Validates traffic splitting, header-based canary targeting,
 * failure injection, automatic rollback thresholds, and canary promotion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Canary Deployment & Release Strategy Tests")
class CanaryDeploymentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CanaryService canaryService;

    @BeforeEach
    void setUp() {
        canaryService.resetMetrics();
    }

    @Test
    @DisplayName("GET /api/canary/status - Should return initial 10% canary traffic allocation")
    void testCanaryInitialStatus() throws Exception {
        mockMvc.perform(get("/api/canary/status")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stableVersion", is("1.0.0")))
            .andExpect(jsonPath("$.canaryVersion", is("2.0.0-canary")))
            .andExpect(jsonPath("$.canaryTrafficWeight", is(10)))
            .andExpect(jsonPath("$.stableTrafficWeight", is(90)))
            .andExpect(jsonPath("$.canaryHealth", is("HEALTHY")))
            .andExpect(jsonPath("$.releaseState", is("ACTIVE_CANARY")));
    }

    @Test
    @DisplayName("GET /api/canary/route with X-Canary: always - Should route explicitly to CANARY")
    void testHeaderBasedRoutingToCanary() throws Exception {
        mockMvc.perform(get("/api/canary/route")
                .header("X-Canary", "always"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.servedBy", is("CANARY")))
            .andExpect(jsonPath("$.version", is("2.0.0-canary")))
            .andExpect(jsonPath("$.status", is("SUCCESS")))
            .andExpect(jsonPath("$.statusCode", is(200)));
    }

    @Test
    @DisplayName("GET /api/canary/route with X-Canary: never - Should route explicitly to STABLE")
    void testHeaderBasedRoutingToStable() throws Exception {
        mockMvc.perform(get("/api/canary/route")
                .header("X-Canary", "never"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.servedBy", is("STABLE")))
            .andExpect(jsonPath("$.version", is("1.0.0")))
            .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    @DisplayName("POST /api/canary/simulate-traffic - Should distribute requests across versions")
    void testSimulateTrafficBatch() throws Exception {
        mockMvc.perform(post("/api/canary/simulate-traffic")
                .param("count", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulatedRequests", is(20)))
            .andExpect(jsonPath("$.configuredCanaryWeight", is("10%")))
            .andExpect(jsonPath("$.routedToCanary", notNullValue()))
            .andExpect(jsonPath("$.routedToStable", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/canary/set-weight - Should dynamically adjust canary percentage")
    void testSetCanaryWeight() throws Exception {
        mockMvc.perform(post("/api/canary/set-weight")
                .param("weight", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canaryTrafficWeight", is(25)))
            .andExpect(jsonPath("$.stableTrafficWeight", is(75)))
            .andExpect(jsonPath("$.releaseState", is("ACTIVE_CANARY")));
    }

    @Test
    @DisplayName("Simulate Canary Outage -> X-Canary should fail with 500, prompting automated rollback")
    void testCanaryFailureAndRollback() throws Exception {
        // 1. Inject failure into Canary
        mockMvc.perform(post("/api/canary/simulate-failure")
                .param("reason", "NullPointer regression in Canary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANARY_FAILURE_INJECTED")));

        // 2. Canary traffic returns 500 Internal Server Error
        mockMvc.perform(get("/api/canary/route")
                .header("X-Canary", "always"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.servedBy", is("CANARY")))
            .andExpect(jsonPath("$.status", is("ERROR")));

        // 3. Status reflects DEGRADED health
        mockMvc.perform(get("/api/canary/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canaryHealth", is("DEGRADED")))
            .andExpect(jsonPath("$.failedCanaryRequests", greaterThanOrEqualTo(1)));

        // 4. Trigger Canary Rollback
        mockMvc.perform(post("/api/canary/rollback")
                .param("reason", "Canary health degraded"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canaryTrafficWeight", is(0)))
            .andExpect(jsonPath("$.releaseState", is("ROLLED_BACK")));

        // 5. Normal traffic now routes 100% to STABLE
        mockMvc.perform(get("/api/canary/route"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.servedBy", is("STABLE")));
    }

    @Test
    @DisplayName("POST /api/canary/promote - Should promote canary version to 100% production stable")
    void testCanaryPromotion() throws Exception {
        mockMvc.perform(post("/api/canary/promote"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stableVersion", is("2.0.0-canary")))
            .andExpect(jsonPath("$.canaryTrafficWeight", is(0)))
            .andExpect(jsonPath("$.releaseState", is("PROMOTED_TO_STABLE")));

        // Following promotion, stable serves the newly promoted version
        mockMvc.perform(get("/api/canary/route"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.servedBy", is("STABLE")))
            .andExpect(jsonPath("$.version", is("2.0.0-canary")));
    }
}

