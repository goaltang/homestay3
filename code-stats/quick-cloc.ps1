# PowerShell script for code statistics
Write-Host "Starting code statistics..." -ForegroundColor Green

# Define excluded directories and included file extensions
$excludeDirs = @(
    "node_modules", "target", ".git", ".idea", ".vscode", 
    "dist", "build", "uploads", "docs", "@", "ppt_draw", 
    "draw", "new_draw", "think-mcp-server", ".cursor", 
    "browser-tools-mcp", "homestay3"
)

$includeExts = @("java", "ts", "vue", "js", "css", "scss", "html")

# Build command parameters
$excludeDirString = $excludeDirs -join ","
$includeExtString = $includeExts -join ","

# Execute cloc command
& .\cloc-2.04.exe . --include-ext=$includeExtString --exclude-dir=$excludeDirString --progress-rate=50

Write-Host ""
Write-Host "Statistics completed!" -ForegroundColor Green 