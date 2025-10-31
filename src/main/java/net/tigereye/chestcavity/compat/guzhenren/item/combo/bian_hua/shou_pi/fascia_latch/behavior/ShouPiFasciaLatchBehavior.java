package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.behavior;

import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator.ShouPiFasciaLatchCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator.ShouPiFasciaLatchCalculator.FasciaParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.tuning.ShouPiFasciaLatchTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboFx;

/** 筋膜锁扣——积累筋膜计数后由玩家主动引爆。 */
public final class ShouPiFasciaLatchBehavior {

  public static final ShouPiFasciaLatchBehavior INSTANCE = new ShouPiFasciaLatchBehavior();
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "shou_pi_fascia_latch");
  private static final ResourceLocation TENACITY_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "modifiers/shou_pi_fascia_tenacity");

  static {
    OrganActivationListeners.register(
        ABILITY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private ShouPiFasciaLatchBehavior() {}

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = ShouPiComboUtil.findOrgan(cc).orElse(ItemStack.EMPTY);
    if (organ.isEmpty()) {
      return;
    }

    boolean hasTigerGu = ShouPiGuOps.hasOrgan(cc, ShouPiGuTuning.HUPI_GU_ID);
    boolean hasTieGuGu = ShouPiGuOps.hasOrgan(cc, ShouPiGuTuning.TIE_GU_GU_ID);
    if (!hasTigerGu && !hasTieGuGu) {
      return;
    }

    var state = ShouPiComboUtil.resolveState(organ);
    ShouPiGuOps.ensureStage(state, cc, organ);

    int fasciaHits = state.getInt(ShouPiGuTuning.KEY_FASCIA_COUNT, 0);
    if (fasciaHits < ShouPiGuTuning.FASCIA_TRIGGER) {
      return;
    }

    MultiCooldown cooldown = ShouPiGuOps.cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry =
        cooldown.entry(ShouPiGuTuning.KEY_FASCIA_COOLDOWN).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }

    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, ShouPiFasciaLatchTuning.ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }

    FasciaParameters params =
        ShouPiFasciaLatchCalculator.compute(fasciaHits, hasTigerGu, hasTieGuGu);

    OrganStateOps.setLong(
        state,
        cc,
        organ,
        ShouPiGuTuning.KEY_FASCIA_ACTIVE_UNTIL,
        now + params.durationTicks(),
        value -> Math.max(0L, value),
        0L);
    OrganStateOps.setInt(
        state,
        cc,
        organ,
        ShouPiGuTuning.KEY_FASCIA_COUNT,
        0,
        value -> Math.max(0, value),
        0);

    entry.setReadyAt(now + params.cooldown());

    ShouPiGuOps.applyShield(player, params.shieldAmount());

    if (params.applyShockwave()) {
      applyShockwave(player, params);
    }
    if (params.grantTenacity()) {
      grantTenacity(player, params);
    }

    if (player.level() instanceof ServerLevel serverLevel) {
      ShouPiComboFx.playFasciaLatch(serverLevel, player.getX(), player.getY(), player.getZ());
    }

    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void applyShockwave(ServerPlayer player, FasciaParameters params) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    AABB box = player.getBoundingBox().inflate(params.shockwaveRadius());
    for (LivingEntity entity :
        serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            box,
            target -> target != player && target.isAlive() && !target.isSpectator())) {
      Vec3 delta = entity.position().subtract(player.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
      if (horizontal.lengthSqr() < 1.0E-4D) {
        continue;
      }
      Vec3 push = horizontal.normalize().scale(params.shockwaveStrength());
      entity.push(push.x, 0.15D, push.z);
      entity.hurtMarked = true;
    }
  }

  private static void grantTenacity(ServerPlayer player, FasciaParameters params) {
    AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (attribute == null) {
      return;
    }
    AttributeModifier modifier =
        new AttributeModifier(
            TENACITY_MODIFIER_ID, params.tenacityKnockbackResist(), Operation.ADD_VALUE);
    AttributeOps.replaceTransient(attribute, TENACITY_MODIFIER_ID, modifier);
    TickOps.schedule(
        player.serverLevel(),
        () -> AttributeOps.removeById(attribute, TENACITY_MODIFIER_ID),
        params.tenacityDurationTicks());
  }
}
