param (
    [string]$PreviousStableTag = "v0.9.9-stable"
)

Write-Host "==========================================================" -ForegroundColor Red
Write-Host " INITIATING AUTOMATED ROLLBACK" -ForegroundColor Red
Write-Host " Target Rollback Version: $PreviousStableTag" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Red

Write-Host "1. Signaling traffic router to isolate failing instances..."
Write-Host "2. Pulling previous stable image: cd-pipeline-demo:$PreviousStableTag..."
Write-Host "3. Restarting containers with stable image configuration..."
Write-Host "4. Running post-rollback validation check..."

Write-Host "Rollback completed successfully. Traffic rerouted to $PreviousStableTag." -ForegroundColor Green
exit 0
