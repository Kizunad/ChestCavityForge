package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;

/**
 * Base slow-tick behaviour for 骨枪蛊：
 * - 读取 bone_growth 通道
 * - 按阈值转换为 Charge 并写入器官 NBT
 * - 参考骨竹蛊的调试播报输出增量
 */
public enum GuQiangguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation BONE_GROWTH_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");
    private static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");

    private static final double SOFT_CAP = 120.0;
    private static final double FALL_OFF = 0.5;
    
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
    private static final SaturationPolicy SOFT_CAP_POLICY = new SaturationPolicy(SOFT_CAP, FALL_OFF);

    private static final String STATE_KEY = "GuQiangCharge";
    private static final double ENERGY_PER_CHARGE = 60.0;
    private static final int MAX_CHARGE = 10;

    private static final ResourceLocation EMERALD_BONE_GROWTH_CHANNEL =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/emerald_bone_growth");

    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");


    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        LinkageChannel growth = ensureChannel(cc,BONE_GROWTH_CHANNEL);
        int current = NBTCharge.getCharge(organ, STATE_KEY);
        int room = Math.max(0, MAX_CHARGE - current);
        if (room <= 0) {
            return;
        }

        double available = growth.get();
        int converted = Math.min(room, (int) Math.floor(available / ENERGY_PER_CHARGE));
        if (converted <= 0) {
            return;
        }

        growth.adjust(-converted * ENERGY_PER_CHARGE);
        int updated = current + converted;
        NBTCharge.setCharge(organ, STATE_KEY, updated);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        sendDebugMessage(converted, updated, growth.get());
    }

    /** 在注册阶段调用一次，确保通道存在并挂载策略。 */
    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannelSoft(cc,BONE_GROWTH_CHANNEL);
        ensureChannel(cc,EMERALD_BONE_GROWTH_CHANNEL);
        ensureChannel(cc,GU_DAO_INCREASE_EFFECT);
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id)
                    .addPolicy(NON_NEGATIVE);
    }

            // --- 通用初始化 ---
    private static LinkageChannel ensureChannelSoft(ChestCavityInstance cc, ResourceLocation id) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(id)
                    .addPolicy(NON_NEGATIVE)
                    .addPolicy(SOFT_CAP_POLICY);
    }


    private static void sendDebugMessage(int gained, int total, double remainingEnergy) {
        ChestCavity.LOGGER.info("[GuQiang] CHARGE +{} -> {} (energy={})", gained, total, String.format("%.1f", remainingEnergy));
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }


        int charge = NBTCharge.getCharge(organ, STATE_KEY);
        sendHitDebug(String.format("hit start damage=%.1f charge=%d", damage, charge));

        if (charge <= 0) {
        sendHitDebug("insufficient charge -> no trigger");
        return damage;
        }

        LinkageChannel guDaoEffChannel = ensureChannel(cc,GU_DAO_INCREASE_EFFECT);
        double guDaoEfficiency = (1 + guDaoEffChannel.get());

            // 消耗 1 点充能（注意下限）
        int updated = Math.max(0, charge - 1);
        NBTCharge.setCharge(organ, STATE_KEY, updated);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);

        applyBleed(target, guDaoEfficiency);
        float bonusDamage = (float)(10.0 * guDaoEfficiency);
        float result = damage + bonusDamage;
        sendHitDebug(String.format("damage +%.1f => %.1f (charge=%d, efficiency=%.2f)", bonusDamage, result, updated, guDaoEfficiency));
        return result;
    }

    private static void applyBleed(LivingEntity target, double guDaoEfficiency) {
        int scaledAmplifier = Math.max(0, (int)Math.round(guDaoEfficiency) - 1);
        Optional<Holder.Reference<net.minecraft.world.effect.MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
        holder.ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, 200, scaledAmplifier, false, true, true)));
    }

    private static void sendHitDebug(String message) {
        ChestCavity.LOGGER.info("[GuQiang] {}", message);
    }
}
