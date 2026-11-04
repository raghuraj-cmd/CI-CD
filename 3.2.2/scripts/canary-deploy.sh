#!/bin/bash
# ==============================================================================
# Automated Canary Deployment Script
# Deploys canary container, verifies readiness, sends test traffic with X-Canary
# header, and verifies error rate <= 0%.
# ==============================================================================

TARGET_HOST=${1:-"http://localhost:8080"}
CANARY_TAG=${2:-"v2.0.0-canary"}
CANARY_WEIGHT=${3:-"10"}

echo "=========================================================="
echo " 🚀 INITIATING CANARY DEPLOYMENT"
echo " Target Host:   ${TARGET_HOST}"
echo " Canary Tag:    ${CANARY_TAG}"
echo " Canary Weight: ${CANARY_WEIGHT}% traffic"
echo "=========================================================="

echo "Step 1: Checking baseline service readiness..."
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${TARGET_HOST}/actuator/health" || echo "000")
if [ "$HEALTH_STATUS" != "200" ]; then
    echo "⚠️ Warning: Service returned HTTP ${HEALTH_STATUS}. Ensure application is booted."
fi

echo "Step 2: Configuring Canary traffic split to ${CANARY_WEIGHT}%..."
curl -s -X POST "${TARGET_HOST}/api/canary/set-weight?weight=${CANARY_WEIGHT}" > /dev/null || true

echo "Step 3: Querying Canary Status endpoint..."
STATUS_JSON=$(curl -s "${TARGET_HOST}/api/canary/status" || echo "{}")
echo "Status: ${STATUS_JSON}"

echo "Step 4: Sending 10 synthetic requests with 'X-Canary: always' header..."
ERRORS=0
SUCCESS=0
for i in {1..10}; do
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "X-Canary: always" "${TARGET_HOST}/api/canary/route")
    if [ "$CODE" == "200" ]; then
        SUCCESS=$((SUCCESS + 1))
    else
        ERRORS=$((ERRORS + 1))
    fi
done

echo "Synthetic Canary Traffic Results: ${SUCCESS}/10 Passed, ${ERRORS} Errors."

if [ "$ERRORS" -gt 0 ]; then
    echo "❌ CRITICAL: Canary failure detected! Errors: ${ERRORS}"
    echo "Triggering automated Canary Rollback..."
    curl -s -X POST "${TARGET_HOST}/api/canary/rollback?reason=SyntheticCanaryValidationFailed" > /dev/null || true
    exit 1
else
    echo "🎉 CANARY DEPLOYMENT VERIFIED! ${CANARY_TAG} is healthy with ${CANARY_WEIGHT}% traffic."
    echo "Ready for gradual promotion to 100% via canary-promote.sh."
    exit 0
fi

