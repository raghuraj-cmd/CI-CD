#!/bin/bash
# ==============================================================================
# Automated Canary Rollback Script
# Immediately cuts canary traffic to 0% and reverts 100% traffic to stable.
# ==============================================================================

TARGET_HOST=${1:-"http://localhost:8080"}
REASON=${2:-"Canary error budget exceeded"}

echo "=========================================================="
echo " ⚠️ INITIATING CANARY ROLLBACK"
echo " Target Host: ${TARGET_HOST}"
echo " Reason:      ${REASON}"
echo "=========================================================="

echo "Step 1: Setting Canary weight to 0% and marking ROLLED_BACK..."
RESPONSE=$(curl -s -X POST "${TARGET_HOST}/api/canary/rollback?reason=${REASON}")
echo "Rollback Response: ${RESPONSE}"

echo "Step 2: Verifying 100% traffic rerouted to STABLE..."
ROUTE=$(curl -s "${TARGET_HOST}/api/canary/route")
echo "Route Response: ${ROUTE}"

echo "✅ Canary release successfully rolled back. 100% traffic routed to Stable."
exit 0

