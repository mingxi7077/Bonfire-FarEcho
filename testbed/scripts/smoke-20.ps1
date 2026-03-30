param(
    [int]$Players = 20,
    [int]$DurationSec = 180,
    [bool]$HandshakeOk = $true,
    [bool]$HudVisible = $true,
    [bool]$WorldMarkerVisible = $true,
    [bool]$NoDoubleDisplay = $true,
    [int]$ClientCrashCount = 0,
    [string]$ReportPath = "../reports/smoke-20.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Split-Path -Parent $MyInvocation.MyCommand.Path) }

function Resolve-OutputPath {
    param([string]$PathValue)
    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }
    return (Join-Path $scriptDir $PathValue)
}

$resolvedReportPath = Resolve-OutputPath -PathValue $ReportPath
$reportDir = Split-Path -Parent $resolvedReportPath
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}

$checks = [ordered]@{
    handshakeOk = $HandshakeOk
    hudVisible = $HudVisible
    worldMarkerVisible = $WorldMarkerVisible
    noDoubleDisplay = $NoDoubleDisplay
    noClientCrash = ($ClientCrashCount -eq 0)
}

$failedChecks = @(
    $checks.GetEnumerator() |
        Where-Object { -not $_.Value } |
        ForEach-Object { $_.Key }
)
$pass = ($failedChecks.Count -eq 0)

$report = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    scenario = "smoke-20"
    players = $Players
    durationSec = $DurationSec
    checks = $checks
    failedChecks = $failedChecks
    clientCrashCount = $ClientCrashCount
    pass = $pass
}

$report | ConvertTo-Json -Depth 5 | Set-Content -Path $resolvedReportPath -Encoding UTF8

if ($pass) {
    Write-Host "[PASS] smoke-20 report => $resolvedReportPath"
}
else {
    Write-Host "[FAIL] smoke-20 report => $resolvedReportPath"
    Write-Host ("Failed checks: " + ($failedChecks -join ", "))
}
