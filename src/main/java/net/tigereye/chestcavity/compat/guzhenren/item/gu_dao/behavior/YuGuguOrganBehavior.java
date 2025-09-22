package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.tigereye.chestcavity.ChestCavity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;

import java.util.List;
import java.util.Locale;

/**
 * Base behavior for 玉骨蛊 (YuGuGu):
 */
public enum YuGuguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");

    private static final ResourceLocation EMERALD_BONE_GROWTH_CHANNEL =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/emerald_bone_growth");

    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");
    private static final ResourceLocation TU_DAO_INCREASE_EFFECT =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect");


    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double SOFT_CAP = 120.0;
    private static final double FALL_OFF = 0.5;
    private static final SaturationPolicy SOFT_CAP_POLICY = new SaturationPolicy(SOFT_CAP, FALL_OFF);

            /** NBT 键：玉骨蛊当前充能 */
    private static final String STATE_KEY = "YuGuCharge";

    /** 每 1 点充能需要消耗的真元值 */
    private static final double ZHENYUAN_PER_CHARGE = 500.0;

    /** 每 1 点充能需要的能量（比如 linkage 通道里的能量值） */
    private static final double ENERGY_PER_CHARGE = 50.0;

    /** 最大充能上限 */
    private static final int MAX_CHARGE = 100;

    /** 玉骨蛊提供的最大效率增益。 */
    private static final double EFFECT_MAX_BONUS = 0.1;

    /** 当无法维持资源时，每次衰减的比例。 */
    private static final double DECAY_FRACTION = 0.25;

    /**
     * Called when the stack is evaluated inside the chest cavity.
     * Registers a removal listener and, on first insert, applies the baseline efficiency bonus.
     */
    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        boolean alreadyRegistered = staleRemovalContexts.removeIf(old -> old.organ == organ && old.listener == this);
        cc.onRemovedListeners.add(new OrganRemovalContext(organ, this));
        if (alreadyRegistered) {
            return;
        }

        int initialCharge = MAX_CHARGE;
        NBTCharge.setCharge(organ, STATE_KEY, initialCharge);
        double appliedEffect = computeEffect(initialCharge);
        applyEffectDelta(cc, appliedEffect);
        sendEquipMessage(appliedEffect);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        int currentCharge = clampCharge(NBTCharge.getCharge(organ, STATE_KEY));
        double previousEffect = computeEffect(currentCharge);

        double totalZhenyuanCost = ZHENYUAN_PER_CHARGE * stackCount;
        double totalEnergyCost = ENERGY_PER_CHARGE * stackCount;

        LinkageChannel boneChannel = ensureChannel(cc, CHANNEL_ID);
        boolean resourcesPaid = false;
        if (boneChannel.get() >= totalEnergyCost) {
            var handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isPresent() && handleOpt.get().consumeScaledZhenyuan(totalZhenyuanCost).isPresent()) {
                resourcesPaid = true;

                boneChannel.adjust(-totalEnergyCost);

                LinkageChannel guDaoEffChannel = ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
                LinkageChannel tuDaoEffChannel = ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
                double totalEfficiency = (1 + guDaoEffChannel.get()) * (1 + tuDaoEffChannel.get());

                LinkageChannel emeraldChannel = ensureChannel(cc, EMERALD_BONE_GROWTH_CHANNEL);
                double before = emeraldChannel.get();
                double gained = totalEfficiency * stackCount;
                emeraldChannel.adjust(gained);

                sendHarvestMessage(stackCount, before, before + gained, totalZhenyuanCost, totalEnergyCost, computeEffect(MAX_CHARGE));
            }
        }

        int updatedCharge = resourcesPaid ? MAX_CHARGE : decayCharge(currentCharge);
        if (updatedCharge != currentCharge) {
            NBTCharge.setCharge(organ, STATE_KEY, updatedCharge);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }

        double updatedEffect = computeEffect(updatedCharge);
        double delta = updatedEffect - previousEffect;
        if (delta != 0.0) {
            applyEffectDelta(cc, delta);
        }

        if (!resourcesPaid && updatedCharge != currentCharge) {
            sendDecayMessage(updatedCharge, updatedEffect);
        }
    }


    /** 在注册阶段调用一次，确保通道存在并挂载策略。 */
    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannelSoft(cc, EMERALD_BONE_GROWTH_CHANNEL);
        ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
        ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
    }

        // --- 通用初始化 ---
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

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        int charge = clampCharge(NBTCharge.getCharge(organ, STATE_KEY));
        double effect = computeEffect(charge);
        if (effect != 0.0) {
            applyEffectDelta(cc, -effect);
            sendRemovalMessage(effect);
        }
        NBTCharge.setCharge(organ, STATE_KEY, 0);
    }


    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }

        // TODO: 玉骨蛊命中逻辑框架 - 后续实现
        return damage;
    }



    private static int clampCharge(int value) {
        return Math.max(0, Math.min(MAX_CHARGE, value));
    }

    private static int decayCharge(int currentCharge) {
        if (currentCharge <= 0) {
            return 0;
        }
        int step = Math.max(1, (int) Math.ceil(currentCharge * DECAY_FRACTION));
        return Math.max(0, currentCharge - step);
    }

    private static double computeEffect(int charge) {
        if (charge <= 0) {
            return 0.0;
        }
        double ratio = Math.min(1.0, charge / (double) MAX_CHARGE);
        return EFFECT_MAX_BONUS * ratio;
    }

    private static void applyEffectDelta(ChestCavityInstance cc, double delta) {
        if (delta == 0.0) {
            return;
        }
        LinkageChannel guDao = ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
        LinkageChannel tuDao = ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
        guDao.adjust(delta);
        tuDao.adjust(delta);
    }

    private static void sendEquipMessage(double effect) {
        ChestCavity.LOGGER.info(String.format(Locale.ROOT, "[YuGugu] equip -> 增效 %.3f", effect));
    }

    private static void sendHarvestMessage(int stackCount, double before, double after,
                                           double consumedZhenyuan, double consumedEnergy, double effect) {
        ChestCavity.LOGGER.info(String.format(
            Locale.ROOT,
            "[YuGugu] +%d EmeraldGrowth %.1f -> %.1f | 真元消耗=%.1f | 能量消耗=%.1f | 增效=%.3f",
            stackCount, before, after, consumedZhenyuan, consumedEnergy, effect
        ));
    }

    private static void sendDecayMessage(int updatedCharge, double effect) {
        ChestCavity.LOGGER.info(String.format(Locale.ROOT,
            "[YuGugu] 资源不足 -> 衰减 charge=%d (增效 %.3f)",
            updatedCharge, effect
        ));
    }

    private static void sendRemovalMessage(double effect) {
        ChestCavity.LOGGER.info(String.format(Locale.ROOT, "[YuGugu] removed -> 撤销增效 %.3f", effect));
    }
}
