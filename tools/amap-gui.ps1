param(
  [ValidateSet("status", "start", "stop")]
  [string]$Action = "status"
)

$ErrorActionPreference = "Stop"

function Get-DotEnvValue {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Path,
    [Parameter(Mandatory = $true)]
    [string]$Name
  )

  if (-not (Test-Path -LiteralPath $Path)) {
    return $null
  }

  foreach ($line in Get-Content -LiteralPath $Path) {
    if ($line -match "^\s*#") {
      continue
    }

    if ($line -match "^\s*$Name\s*=\s*(.*)\s*$") {
      $value = $Matches[1].Trim()

      if (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
      ) {
        $value = $value.Substring(1, $value.Length - 2)
      }

      return $value
    }
  }

  return $null
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendEnvPaths = @(
  (Join-Path $projectRoot "homestay-front\.env.local"),
  (Join-Path $projectRoot "homestay-front\.env")
)

if (-not $env:AMAP_KEY) {
  foreach ($envPath in $frontendEnvPaths) {
    $value = Get-DotEnvValue -Path $envPath -Name "VITE_AMAP_API_KEY"
    if ($value) {
      $env:AMAP_KEY = $value
      break
    }
  }
}

if (-not $env:AMAP_SECURITY_KEY) {
  foreach ($envPath in $frontendEnvPaths) {
    $value = Get-DotEnvValue -Path $envPath -Name "VITE_AMAP_SECURITY_JS_CODE"
    if ($value) {
      $env:AMAP_SECURITY_KEY = $value
      break
    }
  }
}

if (-not (Get-Command amap-gui -ErrorAction SilentlyContinue)) {
  throw "amap-gui is not installed globally. Install @amap-lbs/amap-gui first."
}

if (-not $env:AMAP_KEY) {
  throw "AMAP_KEY is empty. Set AMAP_KEY directly or configure homestay-front/.env.local with VITE_AMAP_API_KEY."
}

Write-Output "Using AMAP_KEY from environment/project config."

switch ($Action) {
  "status" { amap-gui status }
  "start" { amap-gui start }
  "stop" { amap-gui stop }
}
