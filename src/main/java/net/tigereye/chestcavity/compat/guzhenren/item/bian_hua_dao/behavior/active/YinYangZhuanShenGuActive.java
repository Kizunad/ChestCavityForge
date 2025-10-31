package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.yin_yang.YinYangRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

public enum YinYangZhuanShenGuActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        YinYangZhuanShenGuTuning.SKILL_BODY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateAbility(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    if (cc == null || !YinYangDualityOps.hasOrgan(cc)) {
      return;
    }
    long now = player.level().getGameTime();
    YinYangRuntime.activateBodySwitch(player, cc, now);
  }
}
