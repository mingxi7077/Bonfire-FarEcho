# Bonfire FarEcho 快速联调指南 v1

日期：2026-03-18

## 1. 一键产出测试包

在项目根目录执行：

```powershell
.\gradlew.bat assembleTestBundle --no-configuration-cache
```

产物目录：

- `build/test-bundle/server/plugins/bonfire-farecho-server-0.2.0.jar`
- `build/test-bundle/client/mods/farecho-client-0.2.0.jar`
- `build/test-bundle/client/mods-fallback/farecho-client-0.2.0-fallback-dev.jar`

如果 remap 网络异常，可执行：

```powershell
.\gradlew.bat assembleTestBundleOffline --no-configuration-cache
```

## 2. 服务端部署（Paper/Purpur 1.21.8）

1. 将 `bonfire-farecho-server-0.2.0.jar` 放入 `plugins/`
2. 启动一次服务端，确认插件正常加载
3. 检查 `plugins/BonfireFarEcho/config.yml`

## 3. 客户端部署（Fabric 1.21.8）

1. 将 `farecho-client-0.2.0.jar` 放入客户端 `mods/`
2. 启动客户端并进服
3. 进服后执行 `/farecho trace <player>`，确认 `client=true`

## 4. 最小可用测试流程

1. 2 人联调：握手 + HUD + 世界标记 + 回追踪范围不双显
2. `testbed` 执行四脚本：
   - `smoke-20.ps1`
   - `baseline-50.ps1`
   - `estimate-100.ps1`
   - `round2-summary.ps1`
3. 根据 `testbed/reports/round2-summary.md` 判断是否达标

## 5. 常用命令

- `/farecho stats`
- `/farecho trace <player>`
- `/farecho reload`

