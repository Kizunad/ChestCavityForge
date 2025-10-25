# 铁皮蛊（TiePiGu）

金道肌肉位核心器官，聚焦“耐久硬化 + 力量爆发 + 阶段成长”。本文为开发/策划内部规格，所有玩家向描述另行维护。

---

## 1. 模块与放置

- **ID**：`guzhenren:t_tie_pi_gu`
- **逻辑模块**：`compat.guzhenren.item.jin_dao`
- **行为类**：`compat/guzhenren/item/jin_dao/behavior/TiePiGuBehavior.java`
- **注册集中处**：`compat/guzhenren/item/jin_dao/TieDaoOrganRegistry.java`
- **Organscore 资源**：`data/chestcavity/organs/guzhenren/jin_dao/t_tie_pi_gu.json`
- **主动技注册**：`net.tigereye.chestcavity.skill.ActiveSkillRegistry`（含 HUD 图标与冷却提醒 Toast）

---

## 2. 阶段模型

| 阶段 | 阈值（当前 → 下一） | 备注 |
| ---- | ------------------- | ---- |
| 1 → 2 | 120 SP | 解锁阶段 2 强化 |
| 2 → 3 | 360 SP | 解锁沉击增幅 + 硬化真伤 |
| 3 → 4 | 900 SP | 解锁沉击击退抗性 |
| 4 → 5 | 2000 SP | 解锁终阶冷却缩减/持续延长 |

- **增长封顶**：60 秒内最多累计 60 SP（跨来源共享上限）。
- **存储通道**：`LinkageChannel guzhenren:tiepi_sp`；仅服务端写入，客户端以 FOLLOW 方式同步 UI。
- **升级流程**：
  1. 当 `storedSP ≥ threshold(currentPhase)` 设置“可升级”标志，并推送升级提示。
  2. 客户端打开“器官升级确认”对话；确认结果通过 payload 回传。
  3. 服务端确认后 `phase++`，消耗全部 SP，仅保留 `min(30, cappedGain/2)` 作为“余热值”写回通道。
  4. 播放升级音效、动效与 Toast，并同步阶段 + 残余 SP。
- **重载兼容**：`tiepi_sp` 写入胸腔存档；重连/账本重建后恢复阶段与 SP。

---

## 3. SP 来源与限制

| Key | 触发条件 | 基础收益 | 节流/限制 |
| --- | -------- | -------- | -------- |
| `hardening_per_sec` | **硬化**处于战斗或采掘状态的每整秒 | +1 | 单次硬化最多 +12；脱战 >5s 清零当次计数 |
| `heavy_blow_hit` | **沉击**命中实体 | +4 | 未命中不记；命中瞬间即结算 |
| `block_mine` | 破坏矿物/原木方块 | +1 | 0.4s（8 tick）冷却 |
| `ironwall_absorb` | **铁壁**期间每吸收 20 点伤害 | +2 | 同一来源 1s 内只结算一次 |
| `fall_reduction` | “钢筋抗冲”抵消的跌落伤害 | 每 3 点 +1 | 单次至多 +6 |

**反滥用约束**：
- 对同一实体的重复击杀在 10s 内仅记一次；
- 离线不增长，离线期间定时器停止；
- 被动/主动造成的二级伤害（燃烧、雷链等）不重复计入；
- SP 写入统一走 `LedgerOps.adjust(channel)`，并在写入前经由 `LinkagePolicies.clamp(0, threshold5)`。

---

## 4. 技能规格

### 4.1 主动技

1. **硬化（Hardening）**
   - **持续** 15s，**冷却** 30s，真元 `baseCost=60`，按 `ZhenyuanOps.consumeScaled(baseCost, phaseMultiplier)` 取整；期间每 5s -1 饱食、每秒 -2 精力（`ResourceOps.adjustStamina`）。
   - 基础效果：力量 +15%，防御 +10%，挖掘速度 +15%。
   - 阶段强化：
     - Phase 2：力量 +18%，防御 +12%；
     - Phase 3：附加 5% 真实伤害（乘最终伤害）；
     - Phase 5：持续 +3s，冷却 -5s，挖掘速度 +25%。
   - **SP 钩子**：硬化生效的每个“战斗/采掘秒”通过 `hardening_per_sec` 写入 SP；需要 MultiCooldown 跟踪“脱战 5s”窗口。
   - **饱食守卫**：当饱食 ≤3，硬化直接强制结束并写入禁用标志。

2. **铁壁（Ironwall）**
   - **持续** 3s，**冷却** 20s，真元 `baseCost=30`，施放瞬间 -8 精力。
   - 效果：受伤减免 5%，移速 -15%；吸收伤害在 `ironwall_absorb` 钩子中累计 SP。
   - 阶段强化：Phase 2 持续 +0.5s；Phase 3 冷却 -2s。
   - 与 AbsorptionHelper 联动：当铁壁期间吸收值达 20 点时刷新吸收条并写入 SP。

3. **沉击（Heavy Blow）**
   - **准备** 最长 8s，**冷却** 12s，真元 `baseCost=25`。
   - 效果：下一次近战命中 +30% 伤害并附额外击退；命中方块直接破坏（无掉落）。
   - 阶段强化：Phase 3 伤害 +10%；Phase 4 命中后 1s 内获得 +50% 击退抗性。
   - 状态维护：使用 MultiCooldown + 状态位记录“蓄力中/命中结算”。

4. **重拳沉坠（Slam Fist）** — **联动技**
   - 需求：同时安装 *铁皮蛊* + (*铁骨蛊* 或 *精铁骨蛊*)。
   - **扇形近战**，伤害 = 100% 基础攻击 + 沉击剩余增益，冷却 18s，真元 `baseCost=35`。
   - 自带击退，命中时刷新沉击冷却 50%，便于循环。

### 4.2 被动

- **铁质肌纤维**：常驻攻击 +3%，防御 +5%，移速 -3%（AttributeModifier，通过 OrganScores + 行为 buff 双管）。
- **饱食维持**：`onSlowTick` 每 10s -1 饱食；饱食 ≤3 时禁用所有效果/主动技，并显示 `DEBUG` 级日志；饱食 >3 恢复。
- **钢筋抗冲**：摔落/碰撞伤害 -20%，击退抗性 +20%，并向 `fall_reduction` 管道提供 SP。

---

## 5. 运行规则与事件挂接

- **SlowTick（每 20t）**：
  - 执行饱食扣减、硬化战斗/采掘状态检测、60s SP 上限节流、阶段进度 S2C 同步；
  - MultiCooldown 用于 `hardening_per_sec`、`block_mine`、`ironwall_absorb` 速率限制；
  - 维护“饱食 ≤3”禁用状态，并在恢复条件满足时解锁。
- **onHit / onBreak**：
  - 沉击命中实体 or 方块时结算增益并清空状态；
  - `block_mine` 监听方块标签为矿物/原木方块，命中后写入 SP 与统计。
- **onIncomingDamage**：
  - 铁壁期间统计吸收量，触发 `ironwall_absorb`；
  - 记录摔落伤害抵消值写入 `fall_reduction`；
  - 当联动触发（如金钟回响）追加状态/延时。
- **成本缩放**：所有真元消耗统一使用 `ZhenyuanOps.consumeScaled`；精力通过 `ResourceOps`。
- **数据安全**：禁止直接调用 `LinkageManager`、`GuzhenrenResourceBridge` 底层；所有 Ledger 操作走 `LedgerOps`；冷却统一进入 `MultiCooldown`。

---

## 6. Synergy（联动）

1. **铜铁合甲**（铜皮蛊 / 青铜蛊）
   - 硬化激活时额外 +10% 防御；结束后保留 +5% 防御 2s（使用 `MultiCooldown` 叠加窗口）。

2. **骨肉同金**（铁骨蛊 / 精铁骨蛊）
   - 解锁 **重拳沉坠** 主动技；命中时额外对骨道器官广播“骨片震荡”提示。

3. **金钟回响**（金钟蛊）
   - 铁壁期间追加 +50% 击退抗性；本次持续内首次成功格挡后延长 1s，并输出 DEBUG 日志。

4. **雷镀**（雷盾蛊）
   - 雷盾释放雷击时，刷新沉击冷却 50%（每次雷击触发一次）；需监听雷盾事件总线。

5. **锻兵**（剑锋蛊 / 剑指蛊）
   - 硬化期间命中实体时获得 5% 攻速 buff，持续 5s，可刷新。

联动实现统一走 `OrganStateOps` 读取配置与状态，避免重复查询。

---

## 7. Organscores（落地到 JSON）

| Score Key | 值 |
| --------- | -- |
| `chestcavity:strength` | 1.0 |
| `chestcavity:defense` | 0.5 |
| `chestcavity:impact_resistant` | 0.5 |
| `chestcavity:knockback_resistant` | 0.5 |
| `chestcavity:nerves` | 0.25 |
| `chestcavity:speed` | -0.25 |
| `chestcavity:metabolism` | 0.25 |
| `guzhenren:daohen_jindao` | 1.0 |

---

## 8. 同步 & 调试

- 阶段/SP/技能冷却 UI：每 40 tick 推送一次 `TiePiProgressPayload`；发生升级/禁用事件立即同步。
- 关键日志（DEBUG 级）：
  - 阶段升级与余热保留；
  - 饱食不足导致的禁用/恢复；
  - 联动触发（铜铁合甲、骨肉同金、金钟回响、雷镀、锻兵）。
- 诊断命令：`/cc debug tiepi` 输出当前阶段、SP、硬化/铁壁/沉击冷却与联动状态。

---

## 9. 验收清单

- [ ] 账本/联动依赖全部切换至 `LedgerOps`、`ResourceOps`、`MultiCooldown`、`AbsorptionHelper`。
- [ ] `tiepi_sp` 存档重载正确；离线/重连后阶段与 SP 保持一致。
- [ ] 硬化/铁壁/沉击三技能成本、持续与阶段强化符合规格；饱食 ≤3 时全部禁用。
- [ ] SP 来源节流逻辑生效（60s ≤ 60 点，重击/挖掘按冷却计）。
- [ ] 联动效果在对应器官存在时触发，缺失时静默。
- [ ] `./gradlew compileJava`、`./gradlew test` 全绿；进入游戏手动验证升级/护盾/联动链路。
