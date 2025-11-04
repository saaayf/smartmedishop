# PowerShell script to test Recommendation API using Invoke-RestMethod

Write-Host "=== Testing Recommendation API ===" -ForegroundColor Green
Write-Host ""

# 1. Health Check
Write-Host "1. Health Check:" -ForegroundColor Yellow
Invoke-RestMethod -Uri "http://localhost:8001/health" -Method Get | ConvertTo-Json
Write-Host ""

# 2. General Recommendation
Write-Host "2. General Recommendation:" -ForegroundColor Yellow
$body1 = @{
    state = "Angleterre"
    type = "Pansement médical"
    price = 999
    top_n = 10
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8001/recommend" -Method Post -Body $body1 -ContentType "application/json" | ConvertTo-Json
Write-Host ""

# 3. User Recommendation
Write-Host "3. User Recommendation:" -ForegroundColor Yellow
$body2 = @{
    username = "Man"
    top_n = 10
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8001/recommend_user" -Method Post -Body $body2 -ContentType "application/json" | ConvertTo-Json
Write-Host ""

Write-Host "=== Testing Complete ===" -ForegroundColor Green

