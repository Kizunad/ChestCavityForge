package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.calculator.ShouPiGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

public enum ShouPiGuActive implements OrganActivation {
  INSTANCE;

  static {
    OrganActivationListeners.register(
        net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuIds.ACTIVE_DRUM_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateAbility(player, cc);
          }
        });
  }

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiGuCalculator.findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = ShouPiGuCalculator.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuCalculator.cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_ACTIVE_DRUM_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiGuTuning.ACTIVE_DRUM_BASE_COST)
        .ifPresent(
            consumed -> {
              entry.setReadyAt(now + ShouPiGuTuning.ACTIVE_DRUM_COOLDOWN_TICKS);
              state.setLong(
                  ShouPiGuTuning.KEY_ACTIVE_DRUM_EXPIRE,
                  now + ShouPiGuTuning.ACTIVE_DRUM_DURATION_TICKS,
                  value -> Math.max(0L, value),
                  0L);
              ShouPiGuCalculator.applyDrumBuff(player);
              ActiveSkillRegistry.scheduleReadyToast(
                  player, ShouPiGuTuning.ACTIVE_DRUM_ID, entry.getReadyTick(), now);
              NetworkUtil.sendOrganSlotUpdate(cc, organ);
            });
  }
}
