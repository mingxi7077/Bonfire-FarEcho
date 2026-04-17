# Bonfire FarEcho

[English](#english) | [简体中文](#简体中文)

Bonfire FarEcho is a long-range player awareness system for Paper and Fabric.

Bonfire FarEcho 是一个面向 Paper 与 Fabric 的远距离玩家感知系统。

---

## English

Bonfire FarEcho is a long-range player presence system that adds lightweight awareness outside vanilla tracking distance through a server plugin, a Fabric client companion, and a shared protocol layer.

### Project Goals

- Preserve vanilla visibility behavior when players return to normal tracking range.
- Provide low-cost remote awareness instead of full remote entity simulation.
- Keep protocol design and performance tuning separate from gameplay plugin logic.

### Repository Layout

- `server-paper/`: server-side routing, prioritization, and transport hooks
- `client-fabric-1.21.8/`: client HUD, world markers, and ghost rendering
- `shared-protocol/`: packet models and shared codecs
- `testbed/`: repeatable profiling and capacity estimation helpers
- `docs/`: notes and design material

### Build

```powershell
.\gradlew.bat build
```

### License

This repository currently uses the `Bonfire Non-Commercial Source License 1.0`.
See [LICENSE](LICENSE) for the exact terms.

---

## 简体中文

Bonfire FarEcho 是一个远距离玩家存在感系统，通过服务端插件、Fabric 客户端附属模组和共享协议层，让玩家在超出原版追踪距离时仍能获得轻量级感知。

### 项目目标

- 当玩家重新进入正常追踪范围时，保持原版可见性行为不被破坏。
- 提供低成本的远距感知，而不是完整的远距实体模拟。
- 将协议设计与性能调优从具体玩法插件逻辑中拆分出来。

### 仓库结构

- `server-paper/`：服务端路由、优先级与传输钩子
- `client-fabric-1.21.8/`：客户端 HUD、世界标记与幽灵渲染
- `shared-protocol/`：数据包模型与共享编解码
- `testbed/`：可重复压测与容量估算辅助工具
- `docs/`：设计资料与研究记录

### 构建方式

```powershell
.\gradlew.bat build
```

### 授权

本仓库当前采用 `Bonfire Non-Commercial Source License 1.0`。
具体条款见 [LICENSE](LICENSE)。
