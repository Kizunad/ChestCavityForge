# Hun Dao Phase 6 游戏内冒烟测试脚本

制定时间: 2025-11-18  
测试目标: 验证 HUD/通知渲染、网络同步、FX 效果的全链路稳定性，确保 Phase 6 核心功能无崩溃/视觉异常。  
环境要求:  
- Minecraft 1.21.1 NeoForge 最新版  
- ChestCavity + Guzhenren + HunDao 扩展全加载  
- 单人/多人服务器（优先多人验证同步）  
- 客户端配置: 所有 `HunDaoClientConfig` 开关开启（renderHud/notifications 等 = true）  
- 测试账号: 测试玩家（OP 权限） + 观察玩家（非 OP，用于同步验证）  

## 自检前置命令（终端执行）
```
# 在项目根目录执行，确保编译通过
./gradlew clean compileJava check

# 检查 TODO 残留
rg -n "TODO.*Phase 6" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao
rg -n "TODO.*Phase [0-5]" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao

# 检查网络发送点覆盖
rg -n "send|NetworkHelper|sync" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao/middleware

# 启动服务器/客户端，观察日志
# 预期: [HunDaoClientState.tick()] 每 tick 输出一次（调试模式）
```
**预期结果**: 编译通过，TODO=0，发送点覆盖魂焰/魂兽/GuiWu/HunPo。

---

## 测试场景 1: 魂焰 DoT（Soul Flame）
**触发步骤**:  
1. 装备 Hun Dao 物品（胸腔中），右键攻击/使用技能对目标实体（如僵尸）施放魂焰。  
2. 观察目标实体 10 秒。  

**检查点**:  
- [ ] **FX**: 实体周围橙色粒子循环（soulflame_dot_tick.json）。  
- [ ] **HUD (测试玩家)**: 准星对准实体 → 魂焰堆栈指示器显示 "Soul Flame: X stacks (Y sec)"（橙色文字，准星下方）。  
- [ ] **同步 (观察玩家)**: 切换到观察玩家视角，准星对准同一实体 → HUD 显示相同堆栈/时间。  
- [ ] **通知**: 施放成功 → Toast 通知 "Soul Flame applied to [entity]"（WARNING 橙色）。  
- [ ] **客户端日志**: `[SoulFlameSyncPayload]` 接收记录。  

**截图要求**: HUD 魂焰指示 + 粒子效果 + Toast。  
**失败标准**: HUD 不显示/不同步/崩溃。

---

## 测试场景 2: 鬼雾（Gui Wu）
**触发步骤**:  
1. 激活鬼雾技能（使用对应物品/命令）。  
2. 观察 10 秒，期间移动/攻击。  

**检查点**:  
- [ ] **FX**: 绿色雾效粒子（gui_wu_cast.json / gui_wu_pulse.json）。  
- [ ] **HUD (测试玩家)**: 右上角 "Gui Wu: X sec"（绿色文字，魂兽计时器下方）。  
- [ ] **同步 (观察玩家)**: 观察玩家视角 → HUD 显示相同剩余时间。  
- [ ] **通知**: 激活 → SUCCESS 绿色 Toast "Gui Wu activated"。  
- [ ] **过期**: 时间到 → 自动消失，通知 "Gui Wu expired"。  

**截图要求**: HUD 计时器 + 雾效粒子 + Toast 堆叠。  
**失败标准**: 计时不准/不同步/FX 缺失。

---

## 测试场景 3: 魂兽化（Soul Beast）
**触发步骤**:  
1. 满足条件激活魂兽变身。  
2. 观察变身过程 + 持续 10 秒。  

**检查点**:  
- [ ] **FX**: 变身序列（soulbeast_transform_* .json）。  
- [ ] **HUD (测试玩家)**: 右上角 "Soul Beast: X sec"（红色文字）。  
- [ ] **同步 (观察玩家)**: HUD 显示测试玩家魂兽计时。  
- [ ] **通知**: 变身成功 → SUCCESS Toast；失败 → ERROR Toast。  
- [ ] **过期**: 时间到 → 变身解除，HUD 消失。  

**截图要求**: 变身 FX + HUD 红色计时器 + 观察视角 HUD。  
**失败标准**: 变身卡住/HUD 残影/不同步。

---

## 测试场景 4: 魂魄泄露/消耗（Hun Po）
**触发步骤**:  
1. 消耗魂魄（如技能使用/被动泄露）。  
2. 多次调整魂魄值，观察实时变化。  

**检查点**:  
- [ ] **HUD (测试玩家)**: 屏幕下方紫色渐变魂魄条 "Hun Po: X/Y" 更新流畅。  
- [ ] **同步 (观察玩家)**: 观察玩家 HUD 显示测试玩家魂魄条（若支持）。  
- [ ] **通知**: 低魂魄警告 → WARNING Toast "Hun Po low!"。  
- [ ] **边界**: 0% → 红色闪烁；满值 → 无异常。  

**截图要求**: 魂魄条不同值变化 + 低值警告 Toast。  
**失败标准**: 条不更新/不同步/数值错误。

---

## 测试场景 5: 通知系统 & 配置
**触发步骤**:  
1. 触发多种事件（技能成功/失败/状态变更）。  
2. 切换配置: 关闭 `renderNotifications` → 重载 → 通知消失。  

**检查点**:  
- [ ] **Toast 分类**: INFO(灰)/WARNING(橙)/SUCCESS(绿)/ERROR(红)，淡入淡出 3 秒。  
- [ ] **队列**: 同时 5+ 通知 → 堆叠显示，无覆盖。  
- [ ] **配置响应**: 关闭 HUD → 所有元素隐藏；`hideGui` 模式隐藏。  
- [ ] **性能**: 每 tick 渲染无 Lag（F3 观察 TPS）。  

**截图要求**: 多 Toast 堆叠 + 配置前后对比。  
**失败标准**: 通知卡住/颜色错/配置无效。

---

## 测试场景 6: 清除 & 边缘ケース
**触发步骤**:  
1. 应用所有状态后，使用命令/自然过期清除。  
2. 重连客户端。  

**检查点**:  
- [ ] **Clear Payload**: 状态立即从 HUD 清除。  
- [ ] **重连同步**: 重新加入 → 状态恢复正确。  
- [ ] **多人范围**: 远距离 (>64格) → 无不必要同步日志。  

**截图要求**: 前后 HUD 状态 + 日志片段。  
**失败标准**: 幽灵状态/内存泄漏。

---

## 测试完成报告
- [ ] 所有场景通过，无崩溃。  
- [ ] 附上 12+ 截图（按场景命名: soulflame_hud.png 等）。  
- [ ] 客户端/服务器日志片段（Payload 接收/ tick 输出）。  
- [ ] TPS/内存监控：无异常波动。  
- [ ] 签名: 测试者 [姓名] @ [日期]  

**通过标准**: 100% 检查点 ✓，编译自检 0 错误。  
**阻塞问题**: 记录到 `Phase6_Report.md`。  
**后续**: 更新 `Phase6_Report.md` 测试结果 → 标记 Phase6 验收完成。
