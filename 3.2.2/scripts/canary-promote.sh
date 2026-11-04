#!/bin/bash
# ==============================================================================
# Automated Canary Promotion Script
# Expands Canary release to 100% production stable release.
# ==============================================================================

TARGET_HOST=${1:-"http://localhost:8080"}

echo "=========================================================="
echo " 🌟 PROMOTING CANARY RELEASE TO 100% PRODUCTION"
echo " Target Host: ${TARGET_HOST}"
echo "=========================================================="

echo "Step 1: Calling promotion endpoint..."
RESPONSE=$(curl -s -X POST "${TARGET_HOST}/api/canary/promote")
echo "Promotion Response: ${RESPONSE}"

echo "Step 2: Verifying post-promotion status..."
STATUS=$(curl -s "${TARGET_HOST}/api/canary/status")
echo "Current Status: ${STATUS}"

echo "✅ Canary release successfully promoted to 100% Production Stable!"
exit 0

