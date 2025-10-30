package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.behavior;

import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic.BianHuaDaoSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator.RollParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning.ShouPiRollEvasionTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 皮走滚袭——兽皮蛊组合杀招：机动与反击窗口的再利用。 */
public final class ShouPiRollEvasionBehavior {

  public static final ShouPiRollEvasionBehavior INSTANCE = new ShouPiRollEvasionBehavior();
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "shou_pi_roll_evasion");

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private ShouPiRollEvasionBehavior() {}

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiComboUtil.findOrgan(cc).orElse(ItemStack.EMPTY);
    if (organ.isEmpty()) {
      return;
    }
    int synergyCount = ShouPiComboUtil.countArmorSynergy(cc);
    if (synergyCount <= 0) {
      return;
    }

    var state = ShouPiComboUtil.resolveState(organ);
    ShouPiGuOrganBehavior.ensureStage(state, cc, organ);

    MultiCooldown cooldown = ShouPiGuOrganBehavior.cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuOrganBehavior.KEY_ROLL_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }

    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiRollEvasionTuning.ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }

    var snapshot =
        cc.owner
            .getPersistentData()
            .getCompound("SkillEffectBus")
            .getCompound("shou_pi:" + ABILITY_ID.getPath());
    RollParameters params =
        ShouPiRollEvasionCalculator.compute(synergyCount, BianHuaDaoSnapshot.fromNBT(snapshot));

    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      horizontal = new Vec3(1.0D, 0.0D, 0.0D);
    }
    Vec3 offset = horizontal.normalize().scale(params.distance());
    TeleportOps.blinkOffset(player, offset);

    long mitigationWindow = now + Mth.clamp(params.mitigationWindowTicks(), 1, 40);
    state.setLong(
        ShouPiGuOrganBehavior.KEY_ROLL_EXPIRE,
        mitigationWindow,
        value -> Math.max(0L, value),
        0L);
    entry.setReadyAt(now + params.cooldown());

    ShouPiGuOrganBehavior.applyRollCounter(
        player, params.resistanceDurationTicks(), params.resistanceAmplifier());
    ShouPiGuOrganBehavior.applyRollSlow(
        player, params.slowDurationTicks(), params.slowAmplifier(), params.slowRadius());

    // 刷新厚皮窗口，便于下一次受击时立即触发
    OrganStateOps.setBoolean(
        state, cc, organ, ShouPiGuOrganBehavior.KEY_THICK_SKIN_READY, true, false);
    OrganStateOps.setLong(
        state,
        cc,
        organ,
        ShouPiGuOrganBehavior.KEY_THICK_SKIN_EXPIRE,
        now + ShouPiGuOrganBehavior.THICK_SKIN_WINDOW_TICKS,
        value -> Math.max(0L, value),
        0L);

    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }
}
