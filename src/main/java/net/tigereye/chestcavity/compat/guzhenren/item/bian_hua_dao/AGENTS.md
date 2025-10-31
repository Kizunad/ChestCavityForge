# 变化道（bian_hua_dao）迁移计划（方案A：渐进收口）

本计划用于指导将旧的家族目录（yu_lin_gu / yin_yang_zhuan_shen_gu / shou_pi_gu）逐步迁移到与冰雪道一致的分层结构，同时保持编译与功能稳定。

---

## 目标与范围
- 对齐冰雪道分层：behavior/{active, passive, organ} + calculator/ + runtime/ + fx/ + messages/ + tuning/ + state/
- 统一入口：业务侧只调用 `calculator/*Ops` 门面，旧目录先作为实现层保留，避免一次性大爆改。
- 清理旧的主动/被动/行为代理类，统一注册链路，避免早期 classloading 与签名错配。

---

## 当前状态（已完成）
- 主动入口（enum + static 注册）：
  - Yu 系：YuYueActive / YuQunActive / YuShiSummonSharkActive
  - 阴阳：YinYangZhuanShenGuActive
- 被动总线：YuLinGuPassive（slowTick 转发至 Calculator）、YinYangZhuanShenGuPassive（迁至 behavior/passive，逻辑等价）
- Organ 行为壳：YuLinGuOrganBehavior（slowTick 维护召唤）、YinYangZhuanShenGuOrganBehavior（slowTick 桥接）、ShouPiGuOrganBehavior（常量壳）
- OrganRegistry：BianHuaDaoOrganRegistry 登记 YuLinGu/YinYang slowTick 监听
- 公共 Ops：Yu / 阴阳 / 兽皮三套 `*Calculator` 与 `*Tuning` 已统一迁入 `compat/guzhenren/item/bian_hua_dao/behavior/organ|tuning`，item 层全部改为调用对应门面/计算器
- 组合技分层：`combo/bian_hua` 已建立 `behavior/`、`runtime/`、`calculator/`、`tuning/`、`state/`、`messages/`、`fx/` 目录；YuShi/YuQun 行为仅注册并转发到 runtime
- 构建：`./gradlew compileJava` 通过

---

## 迁移策略（方案A：门面优先，渐进替换）
- 原则：
  - 新增/改动代码一律调用 `calculator/*Ops`，禁止直连 `*Calculator`
  - 组合技 Runtime / FX / Messages 维持独立目录，不再回调 item/bian_hua_dao 内部实现
- 目录收口：
  - 2025-xx：完成 Yu / 阴阳 / 兽皮 `calculator/` 与 `tuning/` 统一，同时首批 YuShi / YuQun runtime、fx、messages 落位；旧目录仅保留行为壳待逐步清理

---

## 引用替换清单（第一阶段）
- 搜索与替换（优先级：Yu → 阴阳 → 兽皮）：
  - 旧 → 新门面映射：
    - Yu：`yu_lin_gu.calculator.YuLinGuCalculator` → `calculator.YuLinGuOps`
    - 阴阳：`yin_yang_zhuan_shen_gu.calculator.YinYangZhuanShenGuCalculator` → `calculator.YinYangOps`
    - 兽皮：`shou_pi_gu.calculator.ShouPiGuCalculator` → `calculator.ShouPiGuOps`
  -（2025-xx 更新）Yu/YinYang/ShouPi 的 Calculator 与 Tuning 已统一迁入 `compat/guzhenren/item/bian_hua_dao/behavior/organ|tuning`，组合技与主体共享同一实现。
- 审计命令：
  - `rg -n "yu_lin_gu\.calculator\.YuLinGuCalculator" src/main/java`
  - `rg -n "yin_yang_zhuan_shen_gu\.calculator\.YinYangZhuanShenGuCalculator" src/main/java`
  - `rg -n "shou_pi_gu\.calculator\.ShouPiGuCalculator" src/main/java`

---

## 分层落位（第二阶段）
- Yu 系（鱼跃/鱼群/召鲨）：
  - 行为层：`behavior/active/*` 仅注册与转发；`behavior/organ/YuLinGuOrganBehavior` 负责 slowTick
  - 执行层：位移/推开/召唤/材料校验落到 `runtime/yu_lin_gu/*`
  - 当前进度：YuShiRuntime 全量迁移（待补 FX、单测）；YuQunRuntime 已即时处理（待评估跨 tick 行为）
  - 提示/粒子：`messages/*`、`fx/*` 按技能拆分，YuShiFx 仍缺召唤特效
- 阴阳（切身/双击窗口/锚点/资源池）：
  - 行为层：`behavior/active` + `behavior/passive`；持续把被动逻辑迁至 `behavior/organ/* + runtime/`（以 organ 监听收口）
  - 执行层与提示/粒子：落到 `runtime/yin_yang/*`、`messages/YinYangMessages`、`fx/YinYangFx`
- 兽皮（组合技 + 被动计数/软反池/厚皮）：
  - 组合技：保留 `combo/*` 架构不变；计算落到 `calculator/*`，执行落到 `runtime/shou_pi/*`
  - 被动：按需接入 `behavior/organ` 监听（slowTick/onHurt/onHit），逻辑落至 `runtime/`

---

## 目录收口与删除（第三阶段）
- 已完成：三套 Calculator / Tuning 统一迁移至 `compat/guzhenren/item/bian_hua_dao/behavior/organ` 与 `compat/guzhenren/item/bian_hua_dao/tuning`。
- 待办：根据后续重构进度，分批迁移 runtime/fx/messages 等执行层逻辑，最终缩减行为监听至统一模板。

---

## 一致性规范（必须遵守）
- Attack Ability 注册：enum 单例 + static 注册（已对齐）
- 资源/冷却/护盾：统一 `ResourceOps`、`MultiCooldown`、`AbsorptionHelper`、`LedgerOps`
- DoT Reaction：如需新增 DoT，使用 `DoTTypes` 常量并走 `ReactionRegistry.preApplyDoT(...)`
- 参数快照：激活前需在 `ActivationHookRegistry` 注册快照（如道痕/经验），在行为层读取快照而非实时拉取

---

## 验证与审计
- 编译：`./gradlew compileJava`
- 单测：纯逻辑（calculator 层）优先；运行 `./gradlew test`
- 手测：`./gradlew runClient`（鱼跃/鱼群/召鲨/阴阳切身/召唤生命周期）
- 健全性：无负冷却、账本不重置、卸下不留残留；必要时检查 IncreaseEffectLedger 警告

---

## 待办（建议顺序）
1) 为 `combo/bian_hua/yu_shi/runtime/YuShiRuntime` 补充单元测试，覆盖供品不足 / 真元不足 / 召唤上限分支
2) 落实 `combo/bian_hua/yu_shi/fx/YuShiFx.playSummoned` 粒子与音效
3) 评估 `combo/bian_hua/yu_qun/runtime/YuQunRuntime` 是否需要跨 tick 目标管理或冷却提示
4) 持续审计是否仍有旧 `*Calculator` 直接引用，如发现立即改用 `calculator/*Ops`
5) 阴阳被动逻辑迁入 organ 监听 + runtime，剩余旧目录在完成迁移后删除

> 说明：以上步骤可并行推进，但建议每一桶（Yu/阴阳/兽皮）完成一个“替换→下沉→验证”闭环后再进入下一桶，降低回归成本。
