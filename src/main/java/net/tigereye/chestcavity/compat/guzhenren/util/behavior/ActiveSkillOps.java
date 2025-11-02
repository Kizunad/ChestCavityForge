package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/**
 * 统一的主动技能激活入口（玩家 + 非玩家）。
 *
 * <p>约定：
 * - 玩家：走 ActiveSkillRegistry.trigger → 完整的快照/钩子/提示链路；
 * - 非玩家：直接 OrganActivationListeners.activate（静默，不做资源与提示处理）。
 *
 * <p>本助手仅规范激活路径与类型分流，不引入任何非玩家资源消耗或额外副作用。
 */
public final class ActiveSkillOps {

  private ActiveSkillOps() {}

  /**
   * 为给定实体触发主动技能。
   *
   * @param owner 技能持有者（玩家或任意 LivingEntity）
   * @param skillId 技能 ID（ResourceLocation）
   * @return 是否触发成功
   */
  public static boolean activateFor(LivingEntity owner, ResourceLocation skillId) {
    Objects.requireNonNull(skillId, "skillId");
    if (owner == null || owner.level().isClientSide()) {
      return false;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return false;
    }

    // 玩家：保留完整触发链（快照/钩子/提示）。
    if (owner instanceof ServerPlayer player) {
      ActiveSkillRegistry.TriggerResult r = ActiveSkillRegistry.trigger(player, skillId);
      return r == ActiveSkillRegistry.TriggerResult.SUCCESS;
    }

    // 非玩家：静默触发行为（不做资源/提示），复用 OrganActivationListeners 注册表
    return OrganActivationListeners.activate(skillId, cc);
  }
}
