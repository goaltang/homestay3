# 精确排除目录，避免误排除Java源码
$excludeDirs = "node_modules,target,.git,.idea,.vscode,dist,build,uploads,docs,@,ppt_draw,draw,new_draw,think-mcp-server,.cursor,browser-tools-mcp"
$includeExts = "java,ts,vue,js,css,scss,html"

# 排除根目录下的homestay3目录，但不影响Java包
& .\cloc-2.04.exe .. --include-ext=$includeExts --exclude-dir=$excludeDirs --fullpath --not-match-d="../homestay3$" --progress-rate=50 