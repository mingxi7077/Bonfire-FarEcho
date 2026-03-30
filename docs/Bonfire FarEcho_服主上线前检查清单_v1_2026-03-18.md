# Bonfire FarEcho 服主上线前检查清单 v1

日期：2026-03-18

## A. 环境检查（启动前）

- [ ] 1. 服务端版本为 `Paper/Purpur 1.21.8`
- [ ] 2. 客户端测试端版本为 `Fabric 1.21.8`
- [ ] 3. `server-paper` 插件与 `client-fabric-1.21.8` 模组均为同一构建批次
- [ ] 4. `config.yml` 已确认核心键：`maxTargetsPerObserver`、`near/mid/far.refreshMs`、`maxRadius`、`degradeThresholds`、`debugEnabled`

## B. 功能检查（2~5人联调）

- [ ] 5. 客户端进服后握手成功（`/farecho trace <player>` 显示 `client=true`）
- [ ] 6. 超出原版追踪范围后可看到 HUD/世界标记
- [ ] 7. 回到原版追踪范围后无双显（FarEcho 自动让位）
- [ ] 8. `/farecho reload` 可执行且参数生效（仅 `farecho.admin`）

## C. 性能检查（20/50 人）

- [ ] 9. 执行 `testbed` 四脚本并生成报告：
  - `smoke-20.ps1`
  - `baseline-50.ps1`
  - `estimate-100.ps1`
  - `round2-summary.ps1`
- [ ] 10. `round2-summary` 结果满足：
  - 50 人预算：CPU `<= 1.5 ms/tick`，平均 `<= 6 KB/s/观察者`，峰值 `<= 12 KB/s/观察者`
  - 100 人预估：CPU `<= 3.6 ms/tick`，平均 `<= 14.4 KB/s/观察者`，峰值 `<= 28.8 KB/s/观察者`

## D. 上线观察（首日）

建议首日每 10~15 分钟执行一次：

```text
/farecho stats
```

重点看 `stats(1s)`：

- `targets/s`
- `clippedRatio`
- `packets/s`
- `degraded/s`
- `bytes/s`
- `render/s`

若异常处理顺序：

1. 提高 `far.refreshMs`
2. 提高 `mid.refreshMs`
3. 降低 `maxTargetsPerObserver`
4. 缩小 `maxRadius`
5. 提前触发退化（调低 `degradeThresholds.distance` 或 `degradeThresholds.targetCount`）
