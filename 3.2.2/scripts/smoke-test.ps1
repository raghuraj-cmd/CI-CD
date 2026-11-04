# ==============================================================================
# Automated Smoke Test Script for CD Pipeline (PowerShell)
# ==============================================================================
param (
    [string]$TargetHost = "http://localhost:8080",
    [int]$MaxRetries = 12,
    [int]$IntervalSeconds = 5
)

Write-Host "Starting smoke test verification on: $TargetHost"
Write-Host "Max retries: $MaxRetries, interval: ${IntervalSeconds}s"

for ($i = 1; $i -le $MaxRetries; $i++) {
    Write-Host "[Attempt $i/$MaxRetries] Probing health endpoint..."
    try {
        $healthResponse = Invoke-RestMethod -Uri "$TargetHost/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
        if ($healthResponse.status -eq "UP") {
            Write-Host "Health check passed (Status: UP)!" -ForegroundColor Green

            $smokeResponse = Invoke-RestMethod -Uri "$TargetHost/api/deployments/smoke-test" -Method Get -TimeoutSec 3 -ErrorAction Stop
            Write-Host "Smoke test response: $($smokeResponse | ConvertTo-Json -Compress)"

            if ($smokeResponse.smokeTest -eq "PASSED") {
                try {
                    $canaryStatus = Invoke-RestMethod -Uri "$TargetHost/api/canary/status" -Method Get -TimeoutSec 3 -ErrorAction SilentlyContinue
                    Write-Host "Canary Release Status: $($canaryStatus | ConvertTo-Json -Compress)" -ForegroundColor Cyan
                } catch {}
                Write-Host "SMOKE TEST PASSED! Service is fully operational." -ForegroundColor Green
                exit 0
            }
        }
    } catch {
        Write-Host "Service not ready yet ($($_.Exception.Message)). Waiting ${IntervalSeconds}s..." -ForegroundColor Yellow
    }
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Error "ERROR: Smoke test timed out after $($MaxRetries * $IntervalSeconds) seconds."
exit 1
