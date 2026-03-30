# Bonfire FarEcho

![License](https://img.shields.io/badge/license-BNSL--1.0-red)
![Commercial Use](https://img.shields.io/badge/commercial-use%20by%20written%20permission%20only-critical)
![Stack](https://img.shields.io/badge/stack-Paper%20%2B%20Fabric-brightgreen)
![Java](https://img.shields.io/badge/java-21-orange)
![Status](https://img.shields.io/badge/status-prototype-informational)

Bonfire FarEcho is a long-range player presence system that adds lightweight awareness outside vanilla tracking distance through a server plugin, a Fabric client companion, and a shared protocol layer.

> Non-commercial source-available. Commercial use requires prior written permission via `mingxi7707@qq.com`.

## Repository Layout

- `server-paper/`: server-side routing, prioritization, and transport hooks.
- `client-fabric-1.21.8/`: client HUD, world markers, and ghost rendering.
- `shared-protocol/`: packet models and protocol codecs shared by both sides.
- `testbed/`: repeatable profiling and capacity estimation helpers.

## Goals

- Preserve vanilla visibility behavior when players re-enter normal tracking range.
- Provide low-cost remote awareness instead of full remote entity simulation.
- Keep protocol and performance tuning separate from gameplay plugin logic.

## Build

```powershell
.\gradlew.bat build
```

## Status

- This repository is an active prototype and integration workspace.
- Test artifacts and generated bundles are intentionally excluded from Git.

## License

Bonfire Non-Commercial Source License 1.0

Commercial use is prohibited unless you first obtain written permission from `mingxi7707@qq.com`.
