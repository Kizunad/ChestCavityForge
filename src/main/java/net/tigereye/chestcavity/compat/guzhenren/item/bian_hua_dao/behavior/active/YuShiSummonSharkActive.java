package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuShiSummonSharkSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/** 变化道·饵祭召鲨主动：enum 单例注册 + 转发到技能实现。 */
public enum YuShiSummonSharkActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        YuShiSummonSharkSkill.ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            YuShiSummonSharkSkill.activate(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    YuShiSummonSharkSkill.activate(player, cc);
  }
}

