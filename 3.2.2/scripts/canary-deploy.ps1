param (
    [string]$TargetHost = "http://localhost:8080",
    [string]$CanaryTag = "v2.0.0-canary",
    [int]$CanaryWeight = 10
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🚀 INITIATING CANARY DEPLOYMENT" -ForegroundColor Cyan
Write-Host " Target Host:   $TargetHost" -ForegroundColor White
Write-Host " Canary Tag:    $CanaryTag" -ForegroundColor White
Write-Host " Canary Weight: $CanaryWeight% traffic" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host "Step 1: Checking baseline service readiness..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$TargetHost/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Actuator health status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Warning: Could not reach actuator health endpoint ($($_.Exception.Message))" -ForegroundColor Yellow
}

Write-Host "Step 2: Configuring Canary traffic split to $CanaryWeight%..." -ForegroundColor Yellow
try {
    $status = Invoke-RestMethod -Uri "$TargetHost/api/canary/set-weight?weight=$CanaryWeight" -Method Post -TimeoutSec 5
    Write-Host "✅ Canary state: $($status.releaseState) (Weight: $($status.canaryTrafficWeight)%)" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Error updating canary weight: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "Step 3: Sending 10 synthetic test requests with 'X-Canary: always' header..." -ForegroundColor Yellow
$errors = 0
$success = 0

for ($i = 1; $i -le 10; $i++) {
    try {
        $headers = @{ "X-Canary" = "always" }
        $resp = Invoke-RestMethod -Uri "$TargetHost/api/canary/route" -Headers $headers -Method Get -TimeoutSec 5
        if ($resp.servedBy -eq "CANARY" -and $resp.status -eq "SUCCESS") {
            $success++
        } else {
            $errors++
        }
    } catch {
        $errors++
    }
}

Write-Host "Results: $success/10 Passed, $errors Errors." -ForegroundColor White

if ($errors -gt 0) {
    Write-Host "❌ CRITICAL: Canary verification failed! Errors: $errors" -ForegroundColor Red
    Write-Host "Triggering automated Canary Rollback..." -ForegroundColor Red
    try {
        Invoke-RestMethod -Uri "$TargetHost/api/canary/rollback?reason=SyntheticVerificationErrors" -Method Post | Out-Null
        Write-Host "✅ Canary traffic isolated. Rollback complete." -ForegroundColor Yellow
    } catch {
        Write-Host "Error during rollback call: $($_.Exception.Message)" -ForegroundColor Red
    }
    exit 1
} else {
    Write-Host "🎉 CANARY DEPLOYMENT SUCCESSFUL! Healthy and accepting $CanaryWeight% traffic." -ForegroundColor Green
    exit 0
}

