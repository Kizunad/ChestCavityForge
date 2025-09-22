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
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

// 新增 import
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;

/**
 * Base behavior for 玉骨蛊 (YuGuGu):
 */
public enum YuGuguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {
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

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        double totalZhenyuanCost = ZHENYUAN_PER_CHARGE * stackCount;
        double totalEnergyCost   = ENERGY_PER_CHARGE   * stackCount;

        // 先检查骨道能量通道是否足够
        LinkageChannel boneChannel = ensureChannel(cc, CHANNEL_ID);
        if (boneChannel.get() < totalEnergyCost) {
            return; // 能量不足
        }

        LinkageChannel guDaoEffChannel = ensureChannel(cc, GU_DAO_INCREASE_EFFECT);
        double guDaoEfficiency = (1 + guDaoEffChannel.get());

        LinkageChannel tuDaoEffChannel = ensureChannel(cc, TU_DAO_INCREASE_EFFECT);
        double tuDaoEfficiency = (1 + tuDaoEffChannel.get());

        double totalEfficiency = guDaoEfficiency * tuDaoEfficiency;

        // 扣真元
        var handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return; // 没有真元系统
        }
        var handle = handleOpt.get();
        if (handle.consumeScaledZhenyuan(totalZhenyuanCost).isEmpty()) {
            return; // 真元不足
        }

        // 扣能量
        boneChannel.adjust(-totalEnergyCost);

        // --- EmeraldBoneGrowthChannel 作为唯一的「充能」来源 ---
        LinkageChannel emeraldChannel = ensureChannel(cc, EMERALD_BONE_GROWTH_CHANNEL);
        double before = emeraldChannel.get();
        double gained = totalEfficiency * stackCount;
        emeraldChannel.adjust(gained);

        sendDebugMessage(stackCount, before, before + gained, totalZhenyuanCost, totalEnergyCost);
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
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }

        // TODO: 玉骨蛊命中逻辑框架 - 后续实现
        return damage;
    }



    private static void sendDebugMessage(int stackCount, double before, double after,
                                     double consumedZhenyuan, double consumedEnergy) {
    ChestCavity.LOGGER.info(
        "[YuGugu] +{} (效率×叠加) EmeraldGrowth {:.1f} -> {:.1f} | 真元消耗={:.1f} | 能量消耗={:.1f}",
        stackCount, before, after, consumedZhenyuan, consumedEnergy
    );
}

}