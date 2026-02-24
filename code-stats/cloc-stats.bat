@echo off
echo 正在统计项目代码...
echo.

REM 方案一：快速统计（排除所有不必要的目录）
echo ===========================================
echo 方案一：快速统计（排除构建和依赖目录）
echo ===========================================
cloc-2.04.exe .. ^
    --exclude-dir="node_modules,target,.git,.idea,.vscode,dist,build,uploads,docs,@,ppt_draw,draw,new_draw,think-mcp-server,.cursor,browser-tools-mcp,homestay3" ^
    --exclude-ext="json,lock,log,tmp,temp" ^
    --report-file=cloc-quick-report.txt ^
    --progress-rate=100

echo.
echo ===========================================
echo 方案二：详细统计（按模块分别统计）
echo ===========================================

REM 后端代码统计
echo 后端代码统计：
cloc-2.04.exe ../homestay-backend ^
    --exclude-dir=target,.idea,docs,uploads ^
    --exclude-ext=json,xml,properties,log ^
    --report-file=cloc-backend-report.txt

echo.

REM 前端代码统计
echo 前端代码统计：
cloc-2.04.exe ../homestay-front ^
    --exclude-dir=node_modules,dist,.vscode,docs ^
    --exclude-ext=json,lock,d.ts ^
    --report-file=cloc-frontend-report.txt

echo.

REM 管理后台代码统计
echo 管理后台代码统计：
cloc-2.04.exe ../homestay-admin ^
    --exclude-dir=node_modules,.vscode ^
    --exclude-ext=json,lock,d.ts,tsbuildinfo ^
    --report-file=cloc-admin-report.txt

echo.
echo ===========================================
echo 方案三：超级快速统计（只统计核心代码文件）
echo ===========================================
cloc-2.04.exe .. ^
    --include-ext="java,ts,vue,js,css,scss,html" ^
    --exclude-dir="node_modules,target,.git,.idea,.vscode,dist,build,uploads,docs,@,ppt_draw,draw,new_draw,think-mcp-server,.cursor,browser-tools-mcp,homestay3" ^
    --report-file=cloc-core-report.txt ^
    --progress-rate=50

echo.
echo 统计完成！报告文件已生成：
echo - cloc-quick-report.txt （快速统计）
echo - cloc-backend-report.txt （后端代码）
echo - cloc-frontend-report.txt （前端代码）
echo - cloc-admin-report.txt （管理后台）
echo - cloc-core-report.txt （核心代码）
pause 