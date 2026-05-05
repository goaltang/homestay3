# 启动后端 + 管理后台
$backendPath = Join-Path $PSScriptRoot "homestay-backend"
$adminPath = Join-Path $PSScriptRoot "homestay-admin"

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$adminPath'; npm run dev"

Write-Host "✅ 已启动后端和管理后台的独立 PowerShell 窗口" -ForegroundColor Green
