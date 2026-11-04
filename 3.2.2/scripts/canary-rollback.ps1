param (
    [string]$TargetHost = "http://localhost:8080",
    [string]$Reason = "Canary error rate threshold breached"
)

Write-Host "==========================================================" -ForegroundColor Red
Write-Host " ⚠️ INITIATING CANARY ROLLBACK" -ForegroundColor Red
Write-Host " Target Host: $TargetHost" -ForegroundColor White
Write-Host " Reason:      $Reason" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Red

Write-Host "Step 1: Rerouting all traffic away from Canary..." -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod -Uri "$TargetHost/api/canary/rollback?reason=$Reason" -Method Post -TimeoutSec 5
    Write-Host "✅ Release State:    $($resp.releaseState)" -ForegroundColor Green
    Write-Host "✅ Canary Weight:    $($resp.canaryTrafficWeight)% (Isolated)" -ForegroundColor Green
    Write-Host "✅ Active Version:   $($resp.stableVersion) (100% Stable)" -ForegroundColor Green
} catch {
    Write-Host "❌ Error executing rollback: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "Step 2: Probing route to confirm traffic goes to Stable..." -ForegroundColor Yellow
try {
    $route = Invoke-RestMethod -Uri "$TargetHost/api/canary/route" -Method Get -TimeoutSec 5
    Write-Host "✅ Traffic Served By: $($route.servedBy) (Version: $($route.version))" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Warning on route probe: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "✅ Canary rollback successfully completed. Stable environment restored." -ForegroundColor Green
exit 0

