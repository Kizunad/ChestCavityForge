package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

public enum YinYangZhuanShenGuActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        YinYangZhuanShenGuIds.SKILL_BODY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateAbility(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    YinYangZhuanShenGuBehavior.INSTANCE.activateBody(player, cc);
  }
}
