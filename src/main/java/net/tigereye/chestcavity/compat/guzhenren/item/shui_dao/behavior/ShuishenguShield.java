package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.damage.IncomingDamageShield;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.math.CurveUtil;

/**
 * Encapsulates the shield absorption logic for Shuishengu so it can be reused or tested
 * independently of the broader organ behaviour.
 */
public final class ShuishenguShield implements IncomingDamageShield {

  public static final ShuishenguShield INSTANCE = new ShuishenguShield();

  public static final String STATE_KEY = "ChestCavityShuishengu";
  public static final int BASE_MAX_CHARGE = 20;
  public static final float DAMAGE_REDUCTION = 40.0f;
  public static final double SHIELD_ALPHA = 3.0;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation CHARGE_CHANNEL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shuishengu_charge");
  private static final ClampPolicy UNIT_CLAMP = new ClampPolicy(0.0, 1.0);

  private ShuishenguShield() {}

  @Override
  public ShieldResult absorb(ShieldContext context) {
    float damage = context.incomingDamage();
    if (damage <= 0.0F) {
      return ShieldResult.noShield(damage);
    }

    ItemStack organ = context.organ();
    int stackCount = Math.max(1, organ.getCount());
    int maxCharge = getEffectiveMaxCharge(stackCount);
    int charge = Math.min(NBTCharge.getCharge(organ, STATE_KEY), maxCharge);
    if (charge <= 0) {
      return ShieldResult.noShield(damage);
    }

    float maxReduction = DAMAGE_REDUCTION * stackCount;
    int cost = computeShieldCost(damage, maxReduction, maxCharge);
    if (cost <= 0) {
      return ShieldResult.noShield(damage);
    }
    if (cost > charge) {
      cost = charge;
    }

    float blockedRatio = cost / (float) maxCharge;
    float reduction = Math.min(damage, maxReduction * blockedRatio);
    float remaining = Math.max(0.0F, damage - reduction);

    int remainingCharge = Math.max(0, charge - cost);
    setCharge(organ, remainingCharge, stackCount);
    broadcastChargeRatio(context.chestCavity(), maxCharge, remainingCharge);

    LivingEntity victim = context.victim();
    boolean shieldBroken = remainingCharge == 0;
    playDamageSound(victim, shieldBroken);
    spawnDamageParticles(victim, stackCount, shieldBroken);

    return new ShieldResult(remaining, reduction, remaining <= 0.0F);
  }

  public static int getEffectiveMaxCharge(int stackCount) {
    return BASE_MAX_CHARGE * Math.max(1, stackCount);
  }

  public static void setCharge(ItemStack stack, int value, int stackCount) {
    int clamped = Math.max(0, Math.min(getEffectiveMaxCharge(stackCount), value));
    NBTCharge.setCharge(stack, STATE_KEY, clamped);
  }

  public static void broadcastChargeRatio(
      ChestCavityInstance cc, int maxCharge, int currentCharge) {
    if (cc == null || maxCharge <= 0) {
      return;
    }
    double ratio = CurveUtil.clamp(currentCharge / (double) maxCharge, 0.0D, 1.0D);
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return;
    }
    LinkageChannel channel = LedgerOps.ensureChannel(context, CHARGE_CHANNEL_ID, UNIT_CLAMP);
    if (channel != null) {
      channel.set(ratio);
    }
  }

  private static void playDamageSound(LivingEntity entity, boolean shieldBroken) {
    entity
        .level()
        .playSound(
            null,
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            shieldBroken ? SoundEvents.SHIELD_BREAK : SoundEvents.GENERIC_SPLASH,
            SoundSource.PLAYERS,
            shieldBroken ? 0.8f : 0.5f,
            shieldBroken ? 1.0f : 1.1f);
  }

  private static void spawnDamageParticles(
      LivingEntity entity, int stackCount, boolean shieldBroken) {
    if (!(entity.level() instanceof ServerLevel server)) {
      return;
    }
    double spread = shieldBroken ? 0.9 : 0.6;
    int splashCount = shieldBroken ? 12 + stackCount * 3 : 6 + stackCount * 2;
    server.sendParticles(
        ParticleTypes.SPLASH,
        entity.getX(),
        entity.getY() + entity.getBbHeight() * 0.6,
        entity.getZ(),
        splashCount,
        spread,
        0.3,
        spread,
        0.2);
    server.sendParticles(
        ParticleTypes.BUBBLE_POP,
        entity.getX(),
        entity.getY() + entity.getBbHeight() * 0.6,
        entity.getZ(),
        splashCount / 2,
        spread * 0.5,
        0.2,
        spread * 0.5,
        0.1);
  }

  private static int computeShieldCost(float damage, float reductionPerFullCharge, int maxCharge) {
    if (damage <= 0f) {
      return 0;
    }
    if (damage >= reductionPerFullCharge) {
      return maxCharge;
    }
    double ratio = damage / reductionPerFullCharge;
    double scaled = maxCharge * CurveUtil.expApproach(ratio, SHIELD_ALPHA);
    int rounded = (int) Math.round(scaled);
    return Math.max(1, Math.min(maxCharge, rounded));
  }
}
