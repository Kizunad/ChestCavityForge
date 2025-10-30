package net.tigereye.chestcavity.skill.effects;

import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 技能效果接口：可在技能触发前/后嵌入增益/减益逻辑。
 *
 * <p>约定：
 * - applyPre 只做可回滚的改动，并返回 AppliedHandle 以便 post 阶段根据结果清理或定时失效。
 * - applyPost 可按结果进行结算（例如经验、药水等），不要求返回句柄。
 */
public interface Effect {

  /** 在技能触发前调用。仅做可回滚的改动。 */
  default AppliedHandle applyPre(EffectContext ctx) {
    return null;
  }

  /** 在技能触发完成后调用（无论成功或失败）。 */
  default void applyPost(EffectContext ctx, ActiveSkillRegistry.TriggerResult result) {}
}

