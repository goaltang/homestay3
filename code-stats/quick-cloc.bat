@echo off
REM 超快速代码统计 - 只统计核心代码文件
cloc-2.04.exe .. --include-ext="java,ts,vue,js,css,scss,html" --exclude-dir="node_modules,target,.git,.idea,.vscode,dist,build,uploads,docs,@,ppt_draw,draw,new_draw,think-mcp-server,.cursor,browser-tools-mcp,homestay3" --progress-rate=50 