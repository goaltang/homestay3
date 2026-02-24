# 支付宝二维码生成测试脚本
# 用于验证网络超时修复效果

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$OrderId = 59,
    [string]$Method = "alipay"
)

Write-Host "=== 支付宝二维码生成测试 ===" -ForegroundColor Green
Write-Host "基础URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "订单ID: $OrderId" -ForegroundColor Yellow
Write-Host "支付方式: $Method" -ForegroundColor Yellow
Write-Host ""

# 测试URL
$testUrl = "$BaseUrl/api/payment/$OrderId/create?method=$Method"
Write-Host "测试URL: $testUrl" -ForegroundColor Cyan

# 准备请求头（需要JWT Token）
$headers = @{
    "Content-Type" = "application/json"
    "Accept" = "application/json"
}

# 注意：这里需要实际的JWT Token
Write-Host ""
Write-Host "注意：此脚本需要有效的JWT Token才能正常工作" -ForegroundColor Red
Write-Host "请确保："
Write-Host "1. 后端服务已启动 (http://localhost:8080)" -ForegroundColor White
Write-Host "2. 订单ID存在且状态正确" -ForegroundColor White
Write-Host "3. 用户已登录并有权限访问该订单" -ForegroundColor White
Write-Host ""

try {
    Write-Host "发送POST请求..." -ForegroundColor Yellow
    
    # 测试连接性
    $response = Invoke-WebRequest -Uri $testUrl -Method POST -Headers $headers -TimeoutSec 180
    
    Write-Host "响应状态码: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "响应内容:" -ForegroundColor Green
    Write-Host $response.Content -ForegroundColor White
    
    # 解析响应
    $jsonResponse = $response.Content | ConvertFrom-Json
    
    if ($jsonResponse.success -eq $true) {
        Write-Host ""
        Write-Host "✅ 二维码生成成功！" -ForegroundColor Green
        if ($jsonResponse.qrCode) {
            Write-Host "二维码内容: $($jsonResponse.qrCode)" -ForegroundColor Cyan
        }
    } else {
        Write-Host ""
        Write-Host "❌ 二维码生成失败" -ForegroundColor Red
        Write-Host "错误信息: $($jsonResponse.message)" -ForegroundColor Red
        if ($jsonResponse.errorCode) {
            Write-Host "错误代码: $($jsonResponse.errorCode)" -ForegroundColor Red
        }
    }
    
} catch [System.Net.WebException] {
    $statusCode = $_.Exception.Response.StatusCode
    Write-Host ""
    Write-Host "❌ 网络请求失败" -ForegroundColor Red
    Write-Host "状态码: $statusCode" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($statusCode -eq 401) {
        Write-Host ""
        Write-Host "提示：401错误通常表示需要登录或JWT Token无效" -ForegroundColor Yellow
        Write-Host "请先登录获取有效的Token" -ForegroundColor Yellow
    }
    
} catch [System.TimeoutException] {
    Write-Host ""
    Write-Host "⏰ 请求超时" -ForegroundColor Red
    Write-Host "这可能表示网络修复仍需要进一步优化" -ForegroundColor Yellow
    
} catch {
    Write-Host ""
    Write-Host "❌ 未知错误" -ForegroundColor Red
    Write-Host "错误类型: $($_.Exception.GetType().Name)" -ForegroundColor Red
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 测试完成 ===" -ForegroundColor Green

# 提供使用说明
Write-Host ""
Write-Host "使用说明:" -ForegroundColor Cyan
Write-Host "1. 确保后端服务运行在 http://localhost:8080" -ForegroundColor White
Write-Host "2. 使用有效的订单ID（默认：59）" -ForegroundColor White
Write-Host "3. 如需测试其他订单，使用: .\test-alipay.ps1 -OrderId 123" -ForegroundColor White
Write-Host "4. 如需测试其他服务器，使用: .\test-alipay.ps1 -BaseUrl 'http://other-server:8080'" -ForegroundColor White 