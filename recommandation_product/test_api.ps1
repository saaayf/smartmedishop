# PowerShell script to test the Recommendation API

Write-Host "Testing Recommendation API..." -ForegroundColor Green

# Test Health Endpoint
Write-Host "`n1. Testing /health endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8001/health" -Method Get
    Write-Host "Health Status: $($health.status)" -ForegroundColor Cyan
    Write-Host "Models Loaded: $($health.models_loaded)" -ForegroundColor Cyan
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test General Recommendation
Write-Host "`n2. Testing /recommend endpoint..." -ForegroundColor Yellow
try {
    $body = @{
        state = "Angleterre"
        type = "Pansement médical"
        price = 999
        top_n = 10
    } | ConvertTo-Json

    $recommend = Invoke-RestMethod -Uri "http://localhost:8001/recommend" -Method Post -Body $body -ContentType "application/json"
    Write-Host "Recommendations found: $($recommend.count)" -ForegroundColor Cyan
    Write-Host "First 3 recommendations:" -ForegroundColor Cyan
    $recommend.recommendations | Select-Object -First 3 | Format-Table -AutoSize
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
}

# Test User Recommendation
Write-Host "`n3. Testing /recommend_user endpoint..." -ForegroundColor Yellow
try {
    $body = @{
        username = "Man"
        top_n = 10
    } | ConvertTo-Json

    $userRecommend = Invoke-RestMethod -Uri "http://localhost:8001/recommend_user" -Method Post -Body $body -ContentType "application/json"
    Write-Host "Username: $($userRecommend.username)" -ForegroundColor Cyan
    Write-Host "Recommendations found: $($userRecommend.count)" -ForegroundColor Cyan
    Write-Host "First 3 recommendations:" -ForegroundColor Cyan
    $userRecommend.recommendations | Select-Object -First 3 | Format-Table -AutoSize
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
}

Write-Host "`nTesting complete!" -ForegroundColor Green

