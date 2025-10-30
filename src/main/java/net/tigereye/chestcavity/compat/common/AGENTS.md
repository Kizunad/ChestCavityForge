# compat/common 模块说明（需随改随更）

本文件约束并说明 `src/main/java/net/tigereye/chestcavity/compat/common/` 目录及其子目录内的代码结构、职责与使用方式。该文件的作用域为当前目录树。凡对本目录新增/重构公共工具或修改行为约束，必须同步更新本文件（含新增类的用途、关键约束与示例）。

## 模块概览
- 目标：沉淀“可复用、无外部副作用、服务端优先”的通用工具，供技能、组合杀招、实体行为与物品逻辑复用。
- 设计原则：
  - 纯读写封装与计算优先；副作用（药水、属性、护盾）统一走既有助手类（AttributeOps、AbsorptionHelper、TickOps）。
  - 生命周期清晰：所有临时状态必须有 TTL，并在离线/死亡/换维度时兜底清理。
  - 不持久化到 NBT/附件，除非明确标注并单独评审。

## 目录与职责
- `agent/`
  - `Agent`：对 `LivingEntity` 的统一视图（玩家/NPC 一致）；提供 `asPlayer()`、`chestCavity()`、`serverTime()`、`getAttribute(...)` 等便捷访问。
  - `Agents`：静态工具库。
    - 属性：`applyTransientAttribute(...)` / `removeAttribute(...)`（内部使用 AttributeOps + ResourceLocation ID）。
    - 护盾：`applyAbsorption(...)`（内部使用 AbsorptionHelper，自动容量管理）。
    - 资源：`openResource(...)`/`tryConsumeScaledZhenyuan|Jingli(...)`/`drainHealth(...)`（透传至 GuzhenrenResourceBridge/ResourceOps）。
    - 标签：`collectInventoryFlows(...)`（一次性收集胸腔物品“流派”标签）。
  - `AgentTempState`：运行期临时状态器，仅管理“按 ID 安装的 transient 属性修饰器”。
    - `applyAttributeWithTTL(...)`：安装并在 TTL 到期回滚；重复安装会替换旧的。
    - `revert(...)`/`cleanupAll(...)`：主动或批量回滚。
    - 注意：不做持久化；如需跨进程持久化请使用特定附件能力，且不在本模块实现。

- `skillcalc/`
  - 目的：统一“技能伤害的最终数值计算与明细”，将攻击方与受击方的增益/减益聚合为可预估结果（不直接改变世界状态）。
  - `DamageCalculator`：计算入口（可注册自定义修正规则）。
  - `DamageComputeContext`：只读上下文（攻击者/受击者/基础伤害/skillId/castId/标签）。
  - `DamageResult`：结果与明细（`base/scaled/breakdown/predictedAbsorptionSpent/predictedHealthDamage`）。
  - `SkillDamageModifier`：可插拔规则接口（`apply(ctx,current,sink)`）。
  - `DamageKind`：简单类型标签（MELEE/PROJECTILE/AOE/TRUE_DAMAGE/FIRE/FROST/POISON/ACTIVE_SKILL/COMBO）。
  - 内置规则（按顺序执行）：
    1) `mod/AttackerFlagBonusModifier`：从技能一次性标记（FlagEffect）消费增伤（`one_hit_bonus`、`one_cast_bonus`）。需要调用方传入 `skillId + castId` 才会消费；否则不生效。
    2) `mod/DefenderResistanceModifier`：读取受击者的 vanilla 抗性（DAMAGE_RESISTANCE）按等级近似减伤（每级 20%，最大 80%）。
- `skill/effects/builtin/`
  - `ResourceFieldSnapshotEffect`：在技能 prehook 阶段读取指定 Guzhenren 资源字段，并通过 `SkillEffectBus.putMetadata` 缓存到本次施放的 cast。供技能行为在激活阶段使用（例如按流派经验/道痕动态调整冷却或增益）。
  - `SkillEffectBus.putMetadata/consumeMetadata`：Effect 与行为之间的轻量数据通道。仅在当前 cast 生命周期内有效，消费后即清理。

## 使用示例
- 在伤害结算点：
  ```java
  var result = DamageCalculator.compute(attacker, defender, baseDamage, skillId, castId,
      java.util.Set.of(net.tigereye.chestcavity.compat.common.skillcalc.DamageKind.MELEE));
  float finalHpDamage = (float) result.predictedHealthDamage();
  ```
- 若仅需预估不消费 Flag（不确定 castId）：
  ```java
  var result = DamageCalculator.compute(attacker, defender, baseDamage, skillId, 0L, java.util.Set.of());
  ```
- 应用临时属性（物品/非技能场景）：
  ```java
  Agents.applyTransientAttribute(entity, Attributes.ATTACK_SPEED, ChestCavity.id("compat/temp/as"),
      +0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
  // TTL 自动回滚：
  AgentTempState.applyAttributeWithTTL(entity, Attributes.ATTACK_SPEED,
      ChestCavity.id("compat/temp/as"), +0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 80);
  ```

## 约束与规范（必须遵守）
- SkillCalc（skillcalc/）中的修正规则：
  - 必须“无外部副作用”，不得修改实体/世界，仅返回新数值并写明细。
  - 读取一次性标记（Flag）时，应当“消费”后清理，防止重复累计；仅在提供 `skillId+castId` 情形下生效。
  - 明细标签（`sink.mul/add/clamp` 的 label）使用 `"模块.含义"` 命名，如 `"flag_bonus"`、`"defender.resistance"`，保持稳定以便 UI/日志检索。
  - 新增规则需考虑顺序影响；注册顺序即执行顺序。
- SkillEffectBus metadata：仅用于跨 Effect/行为传递当次施放的中间数据。写入时需提供稳定的 key（建议使用 `skill_prefix:field`），读取使用 `SkillEffectBus.consumeMetadata`，读取后即移除，避免残留。

- Agent/Agents（agent/）中的操作：
  - 属性/护盾操作仅通过 `AttributeOps`/`AbsorptionHelper`，禁止自行保存/覆盖基值或直接写 NBT。
  - 所有临时状态必须由 TTL 管理，并在离线/死亡/换维度时清理（`AgentTempState.cleanupAll` 由上层事件驱动调用）。

## 扩展建议
- 可为不同流派相克/易伤新增 `SkillDamageModifier` 实现，通过 `DamageKind` 与 `AgentContext` 的流派标签结合判断。
- 需要支持更多防御项（例如自定义护盾、特种抗性）时，新增只读近似规则，务必在注释中说明“近似预估，以引擎结算为准”。

## 文档维护要求（重要）
- 新增/修改以下内容必须同步更新本文件：
  - 公共类/公共方法签名、约束或行为语义。
  - SkillCalc 内置规则的执行顺序与解释。
  - 任何临时状态管理策略（TTL/清理）的变化。
- 变更时请在对应小节补充用途、约束与示例，保持“可直接被调用方复用”的可读性。
