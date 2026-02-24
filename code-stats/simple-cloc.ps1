$excludeDirs = "node_modules,target,.git,.idea,.vscode,dist,build,uploads,docs,@,ppt_draw,draw,new_draw,think-mcp-server,.cursor,browser-tools-mcp"
$includeExts = "java,ts,vue,js,css,scss,html"

& .\cloc-2.04.exe .. --include-ext=$includeExts --exclude-dir=$excludeDirs --exclude-lang="JSON,XML" --progress-rate=50 