package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.shou_pi.ShouPiRuntime;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

public enum ShouPiGuActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        ShouPiGuTuning.ACTIVE_DRUM_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateAbility(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    long now = player.level().getGameTime();
    ShouPiRuntime.activateDrum(player, cc, now);
  }
}
