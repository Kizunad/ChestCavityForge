package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.fx.ShuiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.tuning.ShuiTuning;
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

    float maxReduction = ShuiTuning.SHUI_SHEN_DAMAGE_REDUCTION * stackCount;
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
    ShuiFx.playShieldDamageFx(victim, stackCount, shieldBroken);

    return new ShieldResult(remaining, reduction, remaining <= 0.0F);
  }

  public static int getEffectiveMaxCharge(int stackCount) {
    return ShuiTuning.SHUI_SHEN_BASE_MAX_CHARGE * Math.max(1, stackCount);
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

  private static int computeShieldCost(float damage, float reductionPerFullCharge, int maxCharge) {
    if (damage <= 0f) {
      return 0;
    }
    if (damage >= reductionPerFullCharge) {
      return maxCharge;
    }
    double ratio = damage / reductionPerFullCharge;
    double scaled = maxCharge * CurveUtil.expApproach(ratio, ShuiTuning.SHUI_SHEN_SHIELD_ALPHA);
    int rounded = (int) Math.round(scaled);
    return Math.max(1, Math.min(maxCharge, rounded));
  }
}
