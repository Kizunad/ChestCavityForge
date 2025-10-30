package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;
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
        ShouPiGuIds.ACTIVE_DRUM_ID,
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
    ItemStack organ = ShouPiGuOrganBehavior.findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = ShouPiGuOrganBehavior.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOrganBehavior.cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuOrganBehavior.KEY_ACTIVE_DRUM_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiGuOrganBehavior.ACTIVE_DRUM_BASE_COST)
        .ifPresent(
            consumed -> {
              entry.setReadyAt(now + ShouPiGuOrganBehavior.ACTIVE_DRUM_COOLDOWN_TICKS);
              state.setLong(
                  ShouPiGuOrganBehavior.KEY_ACTIVE_DRUM_EXPIRE,
                  now + ShouPiGuOrganBehavior.ACTIVE_DRUM_DURATION_TICKS,
                  value -> Math.max(0L, value),
                  0L);
              ShouPiGuOrganBehavior.INSTANCE.applyDrumBuff(player);
              ActiveSkillRegistry.scheduleReadyToast(
                  player, ShouPiGuIds.ACTIVE_DRUM_ID, entry.getReadyTick(), now);
              NetworkUtil.sendOrganSlotUpdate(cc, organ);
            });
  }
}
