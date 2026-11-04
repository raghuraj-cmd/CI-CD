param (
    [string]$TargetHost = "http://localhost:8080"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🌟 PROMOTING CANARY RELEASE TO 100% PRODUCTION" -ForegroundColor Cyan
Write-Host " Target Host: $TargetHost" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host "Step 1: Promoting Canary to Stable..." -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod -Uri "$TargetHost/api/canary/promote" -Method Post -TimeoutSec 5
    Write-Host "✅ Promoted Version: $($resp.stableVersion)" -ForegroundColor Green
    Write-Host "✅ Release State:    $($resp.releaseState)" -ForegroundColor Green
    Write-Host "✅ Canary Weight:    $($resp.canaryTrafficWeight)%" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to promote canary: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "Step 2: Sending validation request..." -ForegroundColor Yellow
try {
    $route = Invoke-RestMethod -Uri "$TargetHost/api/canary/route" -Method Get -TimeoutSec 5
    Write-Host "✅ Handled by: $($route.servedBy) (Version: $($route.version))" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Warning on validation request: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "🎉 Canary promotion completed successfully!" -ForegroundColor Green
exit 0

