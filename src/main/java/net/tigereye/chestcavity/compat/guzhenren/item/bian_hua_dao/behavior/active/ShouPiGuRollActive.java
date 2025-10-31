package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.active;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.shou_pi.ShouPiRuntime;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

public enum ShouPiGuRollActive {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        ShouPiGuTuning.ACTIVE_ROLL_ID, ShouPiGuRollActive::onActivate);
  }

  public static void onActivate(LivingEntity entity, ChestCavityInstance cc) {
    if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
      return;
    }
    ShouPiRuntime.activateRoll(player, cc, entity.level().getGameTime());
  }
}
