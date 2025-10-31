package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.runtime.shou_pi;

import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 兽皮蛊运行时公共逻辑。 */
public final class ShouPiRuntime {

  private ShouPiRuntime() {}

  public static void activateDrum(ServerPlayer player, ChestCavityInstance cc, long now) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiGuOps.findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = ShouPiGuOps.resolveState(organ);
    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_ACTIVE_DRUM_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiGuTuning.ACTIVE_DRUM_BASE_COST);
    if (consumed.isEmpty()) {
      return;
    }
    entry.setReadyAt(now + ShouPiGuTuning.ACTIVE_DRUM_COOLDOWN_TICKS);
    state.setLong(
        ShouPiGuTuning.KEY_ACTIVE_DRUM_EXPIRE,
        now + ShouPiGuTuning.ACTIVE_DRUM_DURATION_TICKS,
        value -> Math.max(0L, value),
        0L);
    ShouPiGuOps.applyDrumBuff(player);
    ActiveSkillRegistry.scheduleReadyToast(
        player, ShouPiGuTuning.ACTIVE_DRUM_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }
}
