# Bonfire FarEcho Testbed

This module provides scripts for round-2 validation outputs:
- `smoke-20`: 20-player smoke checks (handshake/HUD/world marker/no-double-display).
- `baseline-50`: 50-player budget baseline (CPU + average/peak bandwidth).
- `estimate-100`: 100-player projection from baseline with linear scale + safety factor.
- `round2-summary`: consolidated PASS/FAIL report with tuning hints.

All scripts write reports into `testbed/reports` by default.

## Quick Start

Run from repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./testbed/scripts/smoke-20.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File ./testbed/scripts/baseline-50.ps1 -ObservedCpuMsPerTick 1.42 -ObservedAvgKbPerObserverPerSec 5.8 -ObservedPeakKbPerObserverPerSec 10.6
powershell -NoProfile -ExecutionPolicy Bypass -File ./testbed/scripts/estimate-100.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File ./testbed/scripts/round2-summary.ps1
```

## Output Files

- `testbed/reports/smoke-20.json`
- `testbed/reports/baseline-50.csv`
- `testbed/reports/baseline-50.json`
- `testbed/reports/estimate-100.csv`
- `testbed/reports/estimate-100.json`
- `testbed/reports/round2-summary.md`
- `testbed/reports/round2-summary.json`

## Budget Defaults

- 50-player baseline:
- CPU <= `1.5 ms/tick`
- Avg bandwidth <= `6 KB/s/observer`
- Peak bandwidth <= `12 KB/s/observer`

- 100-player estimate target:
- CPU <= `3.6 ms/tick`
- Avg bandwidth <= `14.4 KB/s/observer`
- Peak bandwidth <= `28.8 KB/s/observer`

## Notes

- `estimate-100.ps1` reads `baseline-50.json` automatically when present.
- Use `/farecho stats` aggregated values as script inputs for consistent calculations.
- Keep scenario duration and movement patterns stable between runs before comparing results.
