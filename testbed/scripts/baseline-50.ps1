param(
    [int]$Players = 50,
    [double]$ObservedCpuMsPerTick = 1.5,
    [double]$ObservedAvgKbPerObserverPerSec = 6.0,
    [double]$ObservedPeakKbPerObserverPerSec = 12.0,
    [double]$CpuBudgetMsPerTick = 1.5,
    [double]$AvgBandwidthBudgetKbPerObserverPerSec = 6.0,
    [double]$PeakBandwidthBudgetKbPerObserverPerSec = 12.0,
    [string]$CsvPath = "../reports/baseline-50.csv",
    [string]$JsonPath = "../reports/baseline-50.json"
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

function Percent {
    param([double]$Observed, [double]$Budget)
    if ($Budget -le 0) {
        return 0.0
    }
    return [Math]::Round(($Observed / $Budget) * 100.0, 2)
}

$resolvedCsvPath = Resolve-OutputPath -PathValue $CsvPath
$resolvedJsonPath = Resolve-OutputPath -PathValue $JsonPath

foreach ($path in @($resolvedCsvPath, $resolvedJsonPath)) {
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

$cpuPass = ($ObservedCpuMsPerTick -le $CpuBudgetMsPerTick)
$avgPass = ($ObservedAvgKbPerObserverPerSec -le $AvgBandwidthBudgetKbPerObserverPerSec)
$peakPass = ($ObservedPeakKbPerObserverPerSec -le $PeakBandwidthBudgetKbPerObserverPerSec)
$overallPass = $cpuPass -and $avgPass -and $peakPass

$rows = @(
    [pscustomobject]@{
        metric = "cpu_ms_per_tick"
        observed = [Math]::Round($ObservedCpuMsPerTick, 4)
        budget = [Math]::Round($CpuBudgetMsPerTick, 4)
        utilizationPct = Percent -Observed $ObservedCpuMsPerTick -Budget $CpuBudgetMsPerTick
        pass = $cpuPass
    },
    [pscustomobject]@{
        metric = "avg_kb_per_observer_per_sec"
        observed = [Math]::Round($ObservedAvgKbPerObserverPerSec, 4)
        budget = [Math]::Round($AvgBandwidthBudgetKbPerObserverPerSec, 4)
        utilizationPct = Percent -Observed $ObservedAvgKbPerObserverPerSec -Budget $AvgBandwidthBudgetKbPerObserverPerSec
        pass = $avgPass
    },
    [pscustomobject]@{
        metric = "peak_kb_per_observer_per_sec"
        observed = [Math]::Round($ObservedPeakKbPerObserverPerSec, 4)
        budget = [Math]::Round($PeakBandwidthBudgetKbPerObserverPerSec, 4)
        utilizationPct = Percent -Observed $ObservedPeakKbPerObserverPerSec -Budget $PeakBandwidthBudgetKbPerObserverPerSec
        pass = $peakPass
    }
)

$rows | Export-Csv -Path $resolvedCsvPath -NoTypeInformation -Encoding UTF8

$suggestions = @()
if (-not $cpuPass) {
    $suggestions += "CPU over budget: increase near/mid/far.refreshMs by 20%-40% and retest."
}
if (-not $avgPass) {
    $suggestions += "Average bandwidth over budget: lower maxTargetsPerObserver and raise mid.refreshMs."
}
if (-not $peakPass) {
    $suggestions += "Peak bandwidth over budget: lower maxRadius or enforce stronger degradeThresholds."
}
if ($suggestions.Count -eq 0) {
    $suggestions += "Baseline budget met. Keep current defaults and proceed to 100-player estimate."
}

$report = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    scenario = "baseline-50"
    players = $Players
    observed = [ordered]@{
        cpuMsPerTick = [Math]::Round($ObservedCpuMsPerTick, 4)
        avgKbPerObserverPerSec = [Math]::Round($ObservedAvgKbPerObserverPerSec, 4)
        peakKbPerObserverPerSec = [Math]::Round($ObservedPeakKbPerObserverPerSec, 4)
    }
    budgets = [ordered]@{
        cpuMsPerTick = [Math]::Round($CpuBudgetMsPerTick, 4)
        avgKbPerObserverPerSec = [Math]::Round($AvgBandwidthBudgetKbPerObserverPerSec, 4)
        peakKbPerObserverPerSec = [Math]::Round($PeakBandwidthBudgetKbPerObserverPerSec, 4)
    }
    utilization = [ordered]@{
        cpuPct = Percent -Observed $ObservedCpuMsPerTick -Budget $CpuBudgetMsPerTick
        avgPct = Percent -Observed $ObservedAvgKbPerObserverPerSec -Budget $AvgBandwidthBudgetKbPerObserverPerSec
        peakPct = Percent -Observed $ObservedPeakKbPerObserverPerSec -Budget $PeakBandwidthBudgetKbPerObserverPerSec
    }
    passes = [ordered]@{
        cpu = $cpuPass
        avgBandwidth = $avgPass
        peakBandwidth = $peakPass
    }
    pass = $overallPass
    suggestions = $suggestions
}

$report | ConvertTo-Json -Depth 6 | Set-Content -Path $resolvedJsonPath -Encoding UTF8

if ($overallPass) {
    Write-Host "[PASS] baseline-50 reports => $resolvedCsvPath ; $resolvedJsonPath"
}
else {
    Write-Host "[FAIL] baseline-50 reports => $resolvedCsvPath ; $resolvedJsonPath"
    Write-Host ("Tuning hints: " + ($suggestions -join " | "))
}
