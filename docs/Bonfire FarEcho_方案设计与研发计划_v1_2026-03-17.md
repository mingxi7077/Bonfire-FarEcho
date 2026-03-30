# Bonfire FarEcho 方案设计与研发计划 v1

日期：2026-03-17  
最近更新：2026-03-18（Round-2 收敛版）

## 1. 项目定位

FarEcho 是“远距玩家回声系统”，目标是在不破坏原版追踪机制的前提下，为超追踪距离玩家提供轻量可视化感知（HUD 与世界标记）。

适配范围：

- 服务端：`Paper/Purpur 1.21.8`
- 客户端：`Fabric 1.21.8`
- Folia：先接口抽象，后专项适配

## 2. 工程结构

- `shared-protocol/`：协议帧、包类型、编解码
- `server-paper/`：服务端插件（握手、兴趣管理、广播、命令）
- `client-fabric-1.21.8/`：客户端模组（收包、缓存、HUD、世界标记）
- `testbed/`：联调/压测脚本与报告汇总
- `docs/`：方案、性能账、运维口径文档

## 3. 固定技术口径

### 3.1 协议与频道

- 频道：`farecho:hello` / `farecho:server_caps` / `farecho:client_caps` / `farecho:delta_batch` / `farecho:remove_batch` / `farecho:debug_stats`
- 帧头：`protocolVersion(u16) + packetType(u8) + sequence(u32) + payloadLength(varint)`

### 3.2 公共接口与核心类型

- 接口：`PacketCodec<T>` / `ProtocolRegistry` / `ProtocolVersion`
- 类型：`EchoSnapshot` / `EchoDeltaBatch` / `EchoRemoveBatch` / `Capabilities` / `DebugStats`

### 3.3 服务端核心接口

- `SnapshotProvider`
- `InterestEngine`
- `BroadcastPolicy`
- `SchedulerFacade`（Folia 预留位）

## 4. MVP 范围定义

### 4.1 必做

- 握手与客户端识别
- 快照缓存与兴趣管理
- 分层刷新（`near/mid/far`）
- 规则过滤（维度/权限/隐身/旁观）
- 退化策略（方向+距离）
- 客户端 HUD 与世界标记
- stats/reload/trace 命令
- testbed 三阶段报告（20/50/100）与汇总

### 4.2 不做

- 完整假体实体渲染
- 高保真动作/装备同步
- Folia 正式运行时适配

## 5. 里程碑状态（截至 2026-03-18）

### 5.1 Round-1：骨架与 MVP 主链路（已完成）

- [x] 多模块工程骨架与统一构建
- [x] 协议与编解码
- [x] 服务端握手、广播、规则过滤、退化
- [x] 客户端收包缓存、HUD、世界标记、去双显
- [x] `/farecho stats` `/farecho reload` `/farecho trace`

### 5.2 Round-2：稳定性与可运维增强（已完成）

- [x] 优先级增强：`combat > team > interaction > hostile > neutral`
- [x] `testbed` 脚本升级：`smoke-20` / `baseline-50` / `estimate-100` / `round2-summary`
- [x] remapJar TLS 缓解参数
- [x] 客户端发布回退任务：`releaseJarFallback`
- [x] 每秒聚合指标口径：`targets/s`、`clippedRatio`、`packets/s`、`degraded/s`、`bytes/s`、`render/s`

### 5.3 Round-3：联调与文档闭环（进行中）

- [x] 性能账文档回填（含每秒口径与排障流程）
- [ ] 接入真实 20/50 人压测输入并回填建议区间
- [ ] 形成上线前“服主操作清单”

## 6. 验收与容量口径

### 6.1 50 人验收

- CPU：FarEcho 额外开销 `<= 1.5 ms/tick`
- 带宽：每观察者平均 `<= 6 KB/s`，峰值 `<= 12 KB/s`
- 功能：超追踪范围可见、回追踪范围无双显

### 6.2 100 人预估

- 按 `50 -> 100` 线性放大并乘 `1.2` 冗余
- 输出容量曲线与推荐调参

## 7. 测试计划

### 7.1 单元测试

- 协议编解码回环一致性
- 兴趣过滤与优先级排序正确性
- 降级阈值触发正确性
- 每秒聚合指标计算正确性

### 7.2 集成测试

- 2 人联调（握手、渲染、去双显）
- 20 人冒烟（稳定性）
- 50 人基线（预算判定）
- 100 人预估（容量建议）

## 8. 运维与发布流程

### 8.1 日常运维

1. 先看 `/farecho stats` 的 `stats(1s)`
2. 再看 `/farecho trace <player>` 定位个体问题
3. 必要时 `reload` 小步调整参数并复测

### 8.2 客户端构建

- 优先：`releaseJarPreferred`（含 remap）
- 回退：`releaseJarFallback`（网络/TLS异常时用于内部联调）

## 9. 风险与约束

- Windows 中文长路径下 Gradle 可能出现假失败，建议使用 `C:\temp\farecho` junction 验证
- `JAVA_HOME` 必须指向 JDK 根目录（例如 `C:\Program Files\Java\jdk-21`）
- Folia 需后续专项适配与实机验证

## 10. 下一轮执行单（建议）

1. 采集真实 20/50 人联调数据并喂给 `testbed/scripts`
2. 将 `round2-summary` 结果回填到性能账中的“建议区间”
3. 增加一份“服主上线前检查清单（10 项）”
4. 启动 Folia 适配分支（先 SchedulerFacade 的运行时探测与落地）
