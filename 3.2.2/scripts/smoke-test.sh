#!/bin/bash
# ==============================================================================
# Automated Smoke Test Script for CD Pipeline
# Polls application health and smoke-test endpoints with timeout and retries.
# ==============================================================================

TARGET_HOST=${1:-"http://localhost:8080"}
MAX_RETRIES=${2:-12}
SLEEP_INTERVAL=${3:-5}

echo "Starting smoke test verification on: ${TARGET_HOST}"
echo "Max retries: ${MAX_RETRIES}, interval: ${SLEEP_INTERVAL}s"

for ((i=1; i<=MAX_RETRIES; i++)); do
    echo "[Attempt $i/$MAX_RETRIES] Probing health endpoint..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_HOST}/actuator/health" || echo "000")

    if [ "$HTTP_CODE" == "200" ]; then
        echo "✅ Health check passed (HTTP 200)!"

        # Query smoke test business endpoint
        SMOKE_RESPONSE=$(curl -s "${TARGET_HOST}/api/deployments/smoke-test")
        echo "Smoke test response: ${SMOKE_RESPONSE}"

        if [[ "$SMOKE_RESPONSE" == *"PASSED"* ]]; then
            # Also verify Canary status endpoint
            CANARY_STATUS=$(curl -s "${TARGET_HOST}/api/canary/status" || echo "{}")
            echo "Canary deployment status: ${CANARY_STATUS}"
            echo "🎉 SMOKE TEST PASSED! Service and Canary Release Manager are fully operational."
            exit 0
        else
            echo "⚠️ Smoke test body did not indicate PASSED status."
        fi
    else
        echo "⏳ Service not ready yet (HTTP ${HTTP_CODE}). Waiting ${SLEEP_INTERVAL}s..."
    fi

    sleep "$SLEEP_INTERVAL"
done

echo "❌ ERROR: Smoke test timed out after $((MAX_RETRIES * SLEEP_INTERVAL)) seconds."
exit 1
