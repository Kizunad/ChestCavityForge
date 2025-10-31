package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuYueSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/** 变化道·鱼跃主动：enum 单例注册 + 转发到技能实现。 */
public enum YuYueActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        YuYueSkill.ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            YuYueSkill.activate(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    YuYueSkill.activate(player, cc);
  }
}

