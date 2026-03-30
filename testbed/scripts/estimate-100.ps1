param(
    [int]$BaselinePlayers = 50,
    [int]$TargetPlayers = 100,
    [double]$BaselineCpuMsPerTick = 1.5,
    [double]$BaselineAvgKbPerObserverPerSec = 6.0,
    [double]$BaselinePeakKbPerObserverPerSec = 12.0,
    [double]$SafetyFactor = 1.2,
    [double]$CpuBudgetMsPerTick = 3.6,
    [double]$AvgBandwidthBudgetKbPerObserverPerSec = 14.4,
    [double]$PeakBandwidthBudgetKbPerObserverPerSec = 28.8,
    [bool]$UseBaselineJson = $true,
    [string]$BaselineJsonPath = "../reports/baseline-50.json",
    [string]$CsvPath = "../reports/estimate-100.csv",
    [string]$JsonPath = "../reports/estimate-100.json"
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

$resolvedBaselineJsonPath = Resolve-OutputPath -PathValue $BaselineJsonPath
$resolvedCsvPath = Resolve-OutputPath -PathValue $CsvPath
$resolvedJsonPath = Resolve-OutputPath -PathValue $JsonPath

foreach ($path in @($resolvedCsvPath, $resolvedJsonPath)) {
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

if ($UseBaselineJson -and (Test-Path $resolvedBaselineJsonPath)) {
    $baseline = Get-Content -Raw $resolvedBaselineJsonPath | ConvertFrom-Json
    if ($null -ne $baseline.players -and $baseline.players -gt 0) {
        $BaselinePlayers = [int]$baseline.players
    }
    if ($null -ne $baseline.observed) {
        if ($null -ne $baseline.observed.cpuMsPerTick) {
            $BaselineCpuMsPerTick = [double]$baseline.observed.cpuMsPerTick
        }
        if ($null -ne $baseline.observed.avgKbPerObserverPerSec) {
            $BaselineAvgKbPerObserverPerSec = [double]$baseline.observed.avgKbPerObserverPerSec
        }
        if ($null -ne $baseline.observed.peakKbPerObserverPerSec) {
            $BaselinePeakKbPerObserverPerSec = [double]$baseline.observed.peakKbPerObserverPerSec
        }
    }
}

if ($BaselinePlayers -le 0 -or $TargetPlayers -le 0) {
    throw "BaselinePlayers and TargetPlayers must be positive."
}

$scale = [double]$TargetPlayers / [double]$BaselinePlayers
$projectedCpu = [Math]::Round($BaselineCpuMsPerTick * $scale * $SafetyFactor, 4)
$projectedAvg = [Math]::Round($BaselineAvgKbPerObserverPerSec * $scale * $SafetyFactor, 4)
$projectedPeak = [Math]::Round($BaselinePeakKbPerObserverPerSec * $scale * $SafetyFactor, 4)

$cpuPass = ($projectedCpu -le $CpuBudgetMsPerTick)
$avgPass = ($projectedAvg -le $AvgBandwidthBudgetKbPerObserverPerSec)
$peakPass = ($projectedPeak -le $PeakBandwidthBudgetKbPerObserverPerSec)
$overallPass = $cpuPass -and $avgPass -and $peakPass

$rows = @(
    [pscustomobject]@{
        metric = "projected_cpu_ms_per_tick"
        projected = $projectedCpu
        budget = [Math]::Round($CpuBudgetMsPerTick, 4)
        utilizationPct = Percent -Observed $projectedCpu -Budget $CpuBudgetMsPerTick
        pass = $cpuPass
    },
    [pscustomobject]@{
        metric = "projected_avg_kb_per_observer_per_sec"
        projected = $projectedAvg
        budget = [Math]::Round($AvgBandwidthBudgetKbPerObserverPerSec, 4)
        utilizationPct = Percent -Observed $projectedAvg -Budget $AvgBandwidthBudgetKbPerObserverPerSec
        pass = $avgPass
    },
    [pscustomobject]@{
        metric = "projected_peak_kb_per_observer_per_sec"
        projected = $projectedPeak
        budget = [Math]::Round($PeakBandwidthBudgetKbPerObserverPerSec, 4)
        utilizationPct = Percent -Observed $projectedPeak -Budget $PeakBandwidthBudgetKbPerObserverPerSec
        pass = $peakPass
    }
)

$rows | Export-Csv -Path $resolvedCsvPath -NoTypeInformation -Encoding UTF8

$suggestions = @()
if (-not $cpuPass) {
    $suggestions += "100-player CPU estimate exceeds budget: reduce maxTargetsPerObserver and increase refresh intervals."
}
if (-not $avgPass) {
    $suggestions += "100-player average bandwidth estimate exceeds budget: tighten interest radius and increase mid.refreshMs."
}
if (-not $peakPass) {
    $suggestions += "100-player peak bandwidth estimate exceeds budget: lower maxRadius and apply aggressive degrade mode earlier."
}
if ($suggestions.Count -eq 0) {
    $suggestions += "100-player estimate remains inside recommended envelope."
}

$report = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    scenario = "estimate-100"
    baselinePlayers = $BaselinePlayers
    targetPlayers = $TargetPlayers
    safetyFactor = $SafetyFactor
    baseline = [ordered]@{
        cpuMsPerTick = [Math]::Round($BaselineCpuMsPerTick, 4)
        avgKbPerObserverPerSec = [Math]::Round($BaselineAvgKbPerObserverPerSec, 4)
        peakKbPerObserverPerSec = [Math]::Round($BaselinePeakKbPerObserverPerSec, 4)
    }
    projected = [ordered]@{
        cpuMsPerTick = $projectedCpu
        avgKbPerObserverPerSec = $projectedAvg
        peakKbPerObserverPerSec = $projectedPeak
    }
    budgets = [ordered]@{
        cpuMsPerTick = [Math]::Round($CpuBudgetMsPerTick, 4)
        avgKbPerObserverPerSec = [Math]::Round($AvgBandwidthBudgetKbPerObserverPerSec, 4)
        peakKbPerObserverPerSec = [Math]::Round($PeakBandwidthBudgetKbPerObserverPerSec, 4)
    }
    utilization = [ordered]@{
        cpuPct = Percent -Observed $projectedCpu -Budget $CpuBudgetMsPerTick
        avgPct = Percent -Observed $projectedAvg -Budget $AvgBandwidthBudgetKbPerObserverPerSec
        peakPct = Percent -Observed $projectedPeak -Budget $PeakBandwidthBudgetKbPerObserverPerSec
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
    Write-Host "[PASS] estimate-100 reports => $resolvedCsvPath ; $resolvedJsonPath"
}
else {
    Write-Host "[FAIL] estimate-100 reports => $resolvedCsvPath ; $resolvedJsonPath"
    Write-Host ("Tuning hints: " + ($suggestions -join " | "))
}
