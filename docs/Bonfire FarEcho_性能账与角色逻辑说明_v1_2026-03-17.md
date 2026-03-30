# Bonfire FarEcho 性能账与角色逻辑说明 v1

日期：2026-03-17  
最近更新：2026-03-18（Round-2 回填）

## 1. 目标与非目标

### 1.1 MVP 目标（当前范围）

- 兼容目标：`Paper/Purpur 1.21.8`（服务端）+ `Fabric 1.21.8`（客户端）
- 显示范围：超出原版实体追踪半径后，继续提供远距感知
- 显示形式：`HUD + 世界标记（particle marker）`
- 协议口径：统一二进制帧格式，不允许 server/client 各自定义线格式
- 验收口径：以 `50 人实测` 为主，给出 `100 人预估` 建议值

### 1.2 非目标（本期不做）

- 不做完整假体实体（ghost-full）渲染
- 不做装备/动作全量同步
- 不做跨平台渲染一致性细节优化（NeoForge 等后续阶段）

## 2. 角色逻辑（玩家/管理员/服主/开发者）

### 2.1 玩家需要知道

- 你看到的是“远距回声信息”，不是可交互实体
- 当真实实体进入原版可见范围时，FarEcho 自动让位，避免双显
- 在拥塞或远距下可能退化成“方向 + 距离”

### 2.2 管理员需要知道

- 用 `/farecho stats` 看总体与每秒聚合指标
- 用 `/farecho trace <player>` 看单玩家是否识别为 FarEcho 客户端、当前被选目标数、优先级分布
- 用 `/farecho reload` 热加载配置（需要 `farecho.admin`）

### 2.3 服主需要知道

- 是否上线看预算，不看“主观感觉”
- 预算优先级：`TPS 安全 > 带宽稳定 > 显示丰富`
- 超预算时按顺序处理：先降级显示，再缩目标数，再收半径

### 2.4 开发者需要知道

- 协议、兴趣管理、广播、渲染必须共用统一口径
- 优先保证可观测性（stats/trace/debug_stats），再做视觉增强
- 每次性能优化必须给出可复现实验输入与指标前后对比

## 3. 协议口径（固定）

### 3.1 频道

- `farecho:hello`
- `farecho:server_caps`
- `farecho:client_caps`
- `farecho:delta_batch`
- `farecho:remove_batch`
- `farecho:debug_stats`

### 3.2 帧头

`protocolVersion(u16) + packetType(u8) + sequence(u32) + payloadLength(varint) + payload`

### 3.3 公共接口与核心类型

- 公共接口：`PacketCodec<T>` / `ProtocolRegistry` / `ProtocolVersion`
- 核心类型：`EchoSnapshot` / `EchoDeltaBatch` / `EchoRemoveBatch` / `Capabilities` / `DebugStats`

## 4. 性能账（统一口径）

### 4.1 50 人实测预算（硬门槛）

- CPU：FarEcho 额外开销 `<= 1.5 ms/tick`
- 带宽：每观察者平均 `<= 6 KB/s`，峰值 `<= 12 KB/s`
- 功能：超追踪范围可见，回到追踪范围不双显

### 4.2 100 人预估（线性 + 20% 冗余）

公式：`baseline * (100 / 50) * 1.2`

- CPU 建议上限：`1.5 * 2 * 1.2 = 3.6 ms/tick`
- 平均带宽建议上限：`6 * 2 * 1.2 = 14.4 KB/s/观察者`
- 峰值带宽建议上限：`12 * 2 * 1.2 = 28.8 KB/s/观察者`

## 5. 降级与刷新策略

### 5.1 分层刷新

- 近层：`near.refreshMs = 250`
- 中层：`mid.refreshMs = 500`
- 远层：`far.refreshMs = 1000`

### 5.2 退化触发

- 距离退化：`degradeThresholds.distance`（默认 `1536`）
- 目标数退化：`degradeThresholds.targetCount`（默认 `48`）
- 退化行为：`directionOnly=true`，客户端只显示方向/距离，不做完整世界标记

## 6. 统一配置键与建议区间

| 配置键 | 默认值 | 建议区间 | 说明 |
|---|---:|---:|---|
| `maxTargetsPerObserver` | 64 | 32~96 | 每观察者最大回声目标数 |
| `near.refreshMs` | 250 | 200~300 | 近层刷新周期 |
| `mid.refreshMs` | 500 | 400~700 | 中层刷新周期 |
| `far.refreshMs` | 1000 | 800~1500 | 远层刷新周期 |
| `maxRadius` | 3072 | 2048~4096 | 最大处理半径 |
| `degradeThresholds.distance` | 1536 | 1200~2000 | 距离触发退化 |
| `degradeThresholds.targetCount` | 48 | 32~64 | 目标数触发退化 |
| `rules.recentCombatWindowMs` | 15000 | 8000~30000 | 战斗优先窗口 |
| `rules.recentInteractionWindowMs` | 30000 | 10000~60000 | 交互优先窗口 |
| `debugEnabled` | false | false/true | 是否下发 debug_stats |

## 7. 运维命令与指标解释

### 7.1 `/farecho stats`

当前输出两段：

- `stats(total)`：启动以来累计值
- `stats(1s)`：最近 1 秒聚合值（当前排障主参考）

`stats(1s)` 指标含义：

- `targets/s`：每秒选中目标总数
- `clippedRatio`：每秒窗口内裁剪比例（被上限截断强度）
- `packets/s`：每秒发包数
- `degraded/s`：每秒退化次数
- `bytes/s`：每秒发送字节
- `render/s`：每秒客户端渲染对象数（服务端估算窗口聚合）

### 7.2 `/farecho trace <player>`

- `client=true/false`：该玩家是否完成 FarEcho 握手识别
- `selected/clipped/priorities`：兴趣筛选结果摘要
- `signals(...)`：近期战斗/交互信号状态（用于优先级解释）

## 8. 排障流程（服主/管理员）

### 8.1 问题：看不到远距目标

1. 检查双方是否同维度
2. 检查目标是否被隐身/旁观/管理员隐藏规则过滤
3. `trace` 确认客户端握手状态
4. 看 `stats(1s)` 的 `clippedRatio` 是否异常升高

### 8.2 问题：延迟高或抖动

1. 看 `stats(1s).packets/s` 与 `bytes/s` 是否过高
2. 先提高 `far.refreshMs`，再调整 `mid.refreshMs`
3. 必要时降低 `maxTargetsPerObserver`

### 8.3 问题：带宽高

1. 降低 `maxTargetsPerObserver`
2. 提高 `mid/far.refreshMs`
3. 缩小 `maxRadius`
4. 提前触发退化（调低 `degradeThresholds.*`）

## 9. 验收口径（上线前必须满足）

- 50 人：CPU/带宽双达标
- 功能：超追踪范围可见，回到追踪范围不双显
- 运维：管理员可通过 `stats/trace` 快速定位“看不到/延迟高/带宽高”
- 测试记录：具备 20 人冒烟、50 人基线、100 人预估报告

## 10. 当前实现状态（截至 2026-03-18）

### 10.1 已落地

- 协议与编解码统一（shared-protocol）
- 服务端握手、兴趣管理、分层刷新、规则过滤、拥塞退化
- 优先级增强：`combat > team > interaction > hostile > neutral`
- 客户端握手收包、缓存过期、HUD、世界标记、真实实体让位
- 命令：`/farecho stats`、`/farecho reload`、`/farecho trace`
- testbed 自动化：`smoke-20`、`baseline-50`、`estimate-100`、`round2-summary`
- TLS 与构建回退：`releaseJarPreferred` + `releaseJarFallback`

### 10.2 本次回填新增口径

- `stats(1s)` 明确为运维主口径
- `debug_stats` 使用每秒聚合值统一下发
- HUD debug 行与服务端每秒口径对齐（含 `bytes`、`render`）

### 10.3 已知环境注意

- Windows 中文长路径下，Gradle 测试可能出现假失败；建议使用 `C:\temp\farecho` junction
- `JAVA_HOME` 必须指向 JDK 根目录（例如 `C:\Program Files\Java\jdk-21`），不能指向 `...\bin`
