param(
    [string]$SmokeReportPath = "../reports/smoke-20.json",
    [string]$BaselineReportPath = "../reports/baseline-50.json",
    [string]$EstimateReportPath = "../reports/estimate-100.json",
    [string]$MarkdownPath = "../reports/round2-summary.md",
    [string]$JsonPath = "../reports/round2-summary.json"
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

function Read-JsonFile {
    param([string]$PathValue)
    if (-not (Test-Path $PathValue)) {
        return $null
    }
    return (Get-Content -Raw $PathValue | ConvertFrom-Json)
}

$resolvedSmokePath = Resolve-OutputPath -PathValue $SmokeReportPath
$resolvedBaselinePath = Resolve-OutputPath -PathValue $BaselineReportPath
$resolvedEstimatePath = Resolve-OutputPath -PathValue $EstimateReportPath
$resolvedMarkdownPath = Resolve-OutputPath -PathValue $MarkdownPath
$resolvedJsonPath = Resolve-OutputPath -PathValue $JsonPath

foreach ($path in @($resolvedMarkdownPath, $resolvedJsonPath)) {
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

$smoke = Read-JsonFile -PathValue $resolvedSmokePath
$baseline = Read-JsonFile -PathValue $resolvedBaselinePath
$estimate = Read-JsonFile -PathValue $resolvedEstimatePath

$missingReports = @()
if ($null -eq $smoke) { $missingReports += "smoke-20" }
if ($null -eq $baseline) { $missingReports += "baseline-50" }
if ($null -eq $estimate) { $missingReports += "estimate-100" }

$smokePass = ($null -ne $smoke -and $smoke.pass -eq $true)
$baselinePass = ($null -ne $baseline -and $baseline.pass -eq $true)
$estimatePass = ($null -ne $estimate -and $estimate.pass -eq $true)
$overallPass = ($smokePass -and $baselinePass -and $estimatePass -and $missingReports.Count -eq 0)

$smokeFailedChecks = @()
if ($null -ne $smoke -and $null -ne $smoke.failedChecks) {
    $smokeFailedChecks = @($smoke.failedChecks | ForEach-Object { [string]$_ })
}

$suggestions = @()
if ($null -ne $smoke -and $smoke.pass -ne $true) {
    foreach ($failed in $smokeFailedChecks) {
        $suggestions += "Smoke failed check: $failed"
    }
}
if ($null -ne $baseline -and $baseline.pass -ne $true) {
    foreach ($hint in $baseline.suggestions) {
        $suggestions += [string]$hint
    }
}
if ($null -ne $estimate -and $estimate.pass -ne $true) {
    foreach ($hint in $estimate.suggestions) {
        $suggestions += [string]$hint
    }
}
if ($suggestions.Count -eq 0) {
    $suggestions += "No immediate tuning action required for current inputs."
}
$suggestions = @($suggestions | Select-Object -Unique)

$statusLabel = if ($overallPass) { "PASS" } else { "FAIL" }

$markdownLines = @(
    "# FarEcho Round-2 Summary",
    "",
    "- Generated: $((Get-Date).ToString('o'))",
    "- Overall: **$statusLabel**",
    ""
)

if ($missingReports.Count -gt 0) {
    $markdownLines += "## Missing Reports"
    foreach ($item in $missingReports) {
        $markdownLines += "- $item"
    }
    $markdownLines += ""
}

$markdownLines += "## smoke-20"
if ($null -eq $smoke) {
    $markdownLines += "- status: missing"
}
else {
    $markdownLines += "- status: " + ($(if ($smokePass) { "pass" } else { "fail" }))
    $markdownLines += "- players: $($smoke.players)"
    if ($smokeFailedChecks.Count -gt 0) {
        $markdownLines += "- failedChecks: $($smokeFailedChecks -join ', ')"
    }
}
$markdownLines += ""

$markdownLines += "## baseline-50"
if ($null -eq $baseline) {
    $markdownLines += "- status: missing"
}
else {
    $markdownLines += "- status: " + ($(if ($baselinePass) { "pass" } else { "fail" }))
    $markdownLines += "- cpu(ms/tick): $($baseline.observed.cpuMsPerTick) / budget $($baseline.budgets.cpuMsPerTick)"
    $markdownLines += "- avg(KB/s/observer): $($baseline.observed.avgKbPerObserverPerSec) / budget $($baseline.budgets.avgKbPerObserverPerSec)"
    $markdownLines += "- peak(KB/s/observer): $($baseline.observed.peakKbPerObserverPerSec) / budget $($baseline.budgets.peakKbPerObserverPerSec)"
}
$markdownLines += ""

$markdownLines += "## estimate-100"
if ($null -eq $estimate) {
    $markdownLines += "- status: missing"
}
else {
    $markdownLines += "- status: " + ($(if ($estimatePass) { "pass" } else { "fail" }))
    $markdownLines += "- projected cpu(ms/tick): $($estimate.projected.cpuMsPerTick) / budget $($estimate.budgets.cpuMsPerTick)"
    $markdownLines += "- projected avg(KB/s/observer): $($estimate.projected.avgKbPerObserverPerSec) / budget $($estimate.budgets.avgKbPerObserverPerSec)"
    $markdownLines += "- projected peak(KB/s/observer): $($estimate.projected.peakKbPerObserverPerSec) / budget $($estimate.budgets.peakKbPerObserverPerSec)"
}
$markdownLines += ""

$markdownLines += "## Tuning Hints"
foreach ($hint in $suggestions) {
    $markdownLines += "- $hint"
}
$markdownLines += ""

($markdownLines -join [Environment]::NewLine) | Set-Content -Path $resolvedMarkdownPath -Encoding UTF8

$summary = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    overallPass = $overallPass
    status = $statusLabel
    missingReports = $missingReports
    smoke = [ordered]@{
        present = ($null -ne $smoke)
        pass = $smokePass
        failedChecks = [string[]]$smokeFailedChecks
    }
    baseline = [ordered]@{
        present = ($null -ne $baseline)
        pass = $baselinePass
        observed = if ($null -ne $baseline) { $baseline.observed } else { $null }
        budgets = if ($null -ne $baseline) { $baseline.budgets } else { $null }
    }
    estimate = [ordered]@{
        present = ($null -ne $estimate)
        pass = $estimatePass
        projected = if ($null -ne $estimate) { $estimate.projected } else { $null }
        budgets = if ($null -ne $estimate) { $estimate.budgets } else { $null }
    }
    suggestions = $suggestions
}

$summary | ConvertTo-Json -Depth 7 | Set-Content -Path $resolvedJsonPath -Encoding UTF8

Write-Host "[SUMMARY] round-2 => $resolvedMarkdownPath ; $resolvedJsonPath"
