# 四转·剑荡蛊 · 配置常量（Markdown 版）

> 说明：本文件为“唯一参数来源”，实现以此为准。若需调参，仅修改此文档并在评审通过后同步实现。

## 触发与节奏

- 玩家 OnHit 初始振幅：A0_PLAYER = 0.10
- 飞剑 OnHit 初始振幅：A0_SWORD = 0.50
- 基础周期（秒）：BASE_PERIOD_SEC = 1.0
- 波前外扩速度（m/s）：RADIAL_SPEED = 8.0
- 振幅衰减率（/s）：DAMPING_PER_SEC = 0.15（A *= e^(−k·Δt)）
- 周期拉伸率（/s）：PERIOD_STRETCH_PER_SEC = 0.10（P *= (1 + s·Δt)）
- 熄灭阈值：MIN_AMPLITUDE = 0.02
- Shockfield 最长寿命（秒）：MAX_LIFETIME_SEC = 10.0
- 二级波包相对速度比例：WAVE_SPEED_SCALE = 0.6

## 干涉相位系数

- 同相增强倍数：CONSTRUCT_MULT = 1.25
- 反相减弱倍数：DESTRUCT_MULT = 0.75
- 同相相位角（度）：CONSTRUCT_PHASE_DEG = 60
- 反相相位角（度）：DESTRUCT_PHASE_DEG = 120

## 伤害公式系数

- 基础伤害：BASE_DMG = 2.0
- 剑道道痕系数：K_JD = 0.015
- 力量分数系数：K_STR = 0.10
- 流派经验系数：K_FLOW = 0.005
- 武器/飞剑阶乘权：K_TIER = 1.0
- 百分比减伤上限：RESIST_PCT_CAP = 0.60
- 护甲固定减伤换算：ARMOR_FLAT = 0.04（每点护甲折 0.04）
- 伤害地板：DMG_FLOOR = 0.20

## 频率限制与软上限

- 同波对同目标命中 CD（秒）：PER_TARGET_WAVE_HIT_CD = 0.25
- DPS 软封顶基线：DPS_CAP_BASE = 30.0（实际封顶=30*(1+JD/500)）

## 资源与耐久

- Burst 真元门槛：BURST_TIER = 4
- 维持每秒消耗（念头）：COST_NIANTOU_PER_SEC = 10
- 维持每秒消耗（精力）：COST_JINGLI_PER_SEC = 10
- 飞剑被主圈触发生成二级波包的耐久成本：FS_DURA_COST_ON_TOUCH_PCT = 0.005（= 0.5% 最大耐久）

## 关键公式（摘要）

- 核心伤害：D_core = A_eff · M_phase · ( Base + JD·K_JD + STR·K_STR + FLOW·K_FLOW + WTier·K_TIER )
- 最终伤害：D_final = max( D_core · (1 − clamp(Resist,0,RESIST_PCT_CAP)) − Armor·ARMOR_FLAT , DMG_FLOOR )
- 软封顶：DPS_agg 超过 DPS_CAP_BASE*(1+JD/500) 的部分按 50% 计

## 排除与节流（摘录）

- 友伤排除：所有者/友方玩家/同队实体/自身飞剑不受伤
- 自波连带排除：OnHit 当帧同 waveId 对 OnHit 目标本体的波伤忽略；所有者对自身波伤永久免疫
- 命中节流：同一“波/目标”间命中冷却 PER_TARGET_WAVE_HIT_CD 秒

## 变更说明

- 调参与平衡需在本清单上修改，并在 MR 中注明“影响：PVP/性能/视觉”的评估结论
- 实现侧不得维护并行的 Java 常量类，以免混淆来源

