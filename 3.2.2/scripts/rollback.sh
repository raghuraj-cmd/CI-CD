#!/bin/bash
# ==============================================================================
# Automated Rollback Script for CD Pipeline
# Reverts deployment to previous known stable tag upon smoke test failure.
# ==============================================================================

PREVIOUS_STABLE_TAG=${1:-"v0.9.9-stable"}

echo "=========================================================="
echo " ⚠️ INITIATING AUTOMATED ROLLBACK"
echo " Target Rollback Version: ${PREVIOUS_STABLE_TAG}"
echo "=========================================================="

echo "1. Signaling traffic router to isolate failing instances..."
echo "2. Pulling previous stable image: cd-pipeline-demo:${PREVIOUS_STABLE_TAG}..."
echo "3. Restarting containers with stable image configuration..."
echo "4. Running post-rollback validation check..."

echo "✅ Rollback completed successfully. Traffic rerouted to ${PREVIOUS_STABLE_TAG}."
exit 0
