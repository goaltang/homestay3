# 启动前后端开发服务
$backendPath = Join-Path $PSScriptRoot "homestay-backend"
$frontendPath = Join-Path $PSScriptRoot "homestay-front"

# 启动后端（新窗口）
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; mvn spring-boot:run"

# 启动前端（新窗口）
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; npm run dev"

Write-Host "✅ 已启动后端和前端的独立 PowerShell 窗口" -ForegroundColor Green
