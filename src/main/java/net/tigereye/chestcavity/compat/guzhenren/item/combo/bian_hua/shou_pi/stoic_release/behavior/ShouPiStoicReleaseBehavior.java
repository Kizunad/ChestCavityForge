package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.behavior;

import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator.StoicParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.tuning.ShouPiStoicReleaseTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 坚忍释放——将累积的坚忍层数主动引爆。 */
public final class ShouPiStoicReleaseBehavior {

  public static final ShouPiStoicReleaseBehavior INSTANCE = new ShouPiStoicReleaseBehavior();
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "shou_pi_stoic_release");

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private ShouPiStoicReleaseBehavior() {}

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiComboUtil.findOrgan(cc).orElse(ItemStack.EMPTY);
    if (organ.isEmpty()) {
      return;
    }

    int synergy = ShouPiComboUtil.countArmorSynergy(cc);
    if (synergy <= 0) {
      return;
    }

    var state = ShouPiComboUtil.resolveState(organ);
    ShouPiGuOrganBehavior.ensureStage(state, cc, organ);

    if (!state.getBoolean(ShouPiGuOrganBehavior.KEY_STOIC_READY, false)) {
      return;
    }
    long lockUntil = state.getLong(ShouPiGuOrganBehavior.KEY_STOIC_LOCK_UNTIL, 0L);
    long now = player.level().getGameTime();
    if (lockUntil > now) {
      return;
    }

    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiStoicReleaseTuning.ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }

    MultiCooldown cooldown = ShouPiGuOrganBehavior.cooldown(cc, organ, state);
    // 使用专用 entry 记录窗口，便于 UI 提示
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuOrganBehavior.KEY_STOIC_LOCK_UNTIL).withDefault(0L);

    ShouPiGuOrganBehavior.TierParameters tierParams =
        ShouPiGuOrganBehavior.tierParameters(state);
    StoicParameters params = ShouPiStoicReleaseCalculator.compute(tierParams);

    OrganStateOps.setBoolean(
        state, cc, organ, ShouPiGuOrganBehavior.KEY_STOIC_READY, false, false);
    OrganStateOps.setInt(
        state,
        cc,
        organ,
        ShouPiGuOrganBehavior.KEY_STOIC_STACKS,
        0,
        value -> Math.max(0, value),
        0);
    OrganStateOps.setDouble(
        state,
        cc,
        organ,
        ShouPiGuOrganBehavior.KEY_STOIC_ACCUM,
        0.0D,
        value -> Math.max(0.0D, value),
        0.0D);

    OrganStateOps.setLong(
        state,
        cc,
        organ,
        ShouPiGuOrganBehavior.KEY_STOIC_ACTIVE_UNTIL,
        now + params.activeDurationTicks(),
        value -> Math.max(0L, value),
        0L);
    OrganStateOps.setLong(
        state,
        cc,
        organ,
        ShouPiGuOrganBehavior.KEY_STOIC_LOCK_UNTIL,
        now + params.lockTicks(),
        value -> Math.max(0L, value),
        0L);
    entry.setReadyAt(now + params.lockTicks());

    if (params.softReflectBonus() > 0.0D) {
      OrganStateOps.setDouble(
          state,
          cc,
          organ,
          ShouPiGuOrganBehavior.KEY_SOFT_TEMP_BONUS,
          params.softReflectBonus(),
          value -> Math.max(0.0D, value),
          0.0D);
      OrganStateOps.setLong(
          state,
          cc,
          organ,
          ShouPiGuOrganBehavior.KEY_SOFT_TEMP_BONUS_EXPIRE,
          now + params.activeDurationTicks(),
          value -> Math.max(0L, value),
          0L);
    }

    ShouPiGuOrganBehavior.applyShield(player, params.shieldAmount());
    if (params.applySlowAura()) {
      ShouPiGuOrganBehavior.applyStoicSlow(player);
    }

    player
        .level()
        .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ANVIL_USE,
            SoundSource.PLAYERS,
            0.7F,
            1.0F + player.getRandom().nextFloat() * 0.1F);

    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }
}

