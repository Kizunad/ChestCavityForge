package net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.registration.CCItems;

import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;

import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Locale;
import java.util.Objects;

/**
 * Behaviour for 剑脊藤 organ stacks. Gradually charges by draining resources or
 * health from the owning entity.
 */
public enum JianjitengOrganBehavior implements OrganSlowTickListener {
    INSTANCE;


    private static final String MOD_ID = "guzhenren";

    private static final String STATE_KEY = "JianjitengCharge";
    private static final int MAX_CHARGE = 100;

    private static final float HEALTH_COST = 0.1f;
    private static final float MINIMUM_HEALTH_RESERVE = 1.0f;
    private static final double ZHENYUAN_COST = 2.0;

    // TODO: Reintroduce Jianjiteng linkage energy consumption once a producer exists.


    private static final int BONUS_ROLL = 100;
    private static final Component BONUS_MESSAGE = Component.translatable("message.guzhenren.jianjiteng.bonus");
    private static final String LOG_PREFIX = "[Jianjiteng]";

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance chestCavity, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (chestCavity == null) {
            return;
        }


        int currentCharge = clampCharge(NBTCharge.getCharge(organ, STATE_KEY));

        if (!hasHealthReserve(entity)) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} health below safety threshold", LOG_PREFIX, describe(entity));
            }
            return;
        }


        ConsumptionResult payment = null;
        if (entity instanceof Player player) {
            payment = GuzhenrenResourceCostHelper.consumeStrict(player, ZHENYUAN_COST, 0.0);
            if (!payment.succeeded()) {
                if (ChestCavity.LOGGER.isDebugEnabled()) {
                    ChestCavity.LOGGER.debug("{} {} lacks zhenyuan for charging (reason={})", LOG_PREFIX,
                            describe(player), payment.failureReason());
                }
                return;
            }
        }

        if (!drainHealth(entity)) {
            if (entity instanceof Player player) {
                GuzhenrenResourceCostHelper.refund(player, Objects.requireNonNull(payment));
            }
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} health drain failed", LOG_PREFIX, describe(entity));
            }
            return;
        }


        int updatedCharge = currentCharge + 1;
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            if (payment != null && payment.mode() == GuzhenrenResourceCostHelper.Mode.PLAYER_RESOURCES) {
                double consumed = payment.zhenyuanSpent();
                ChestCavity.LOGGER.debug(
                        "{} {} charge -> {}/{} (消耗真元 {})",
                        LOG_PREFIX,
                        describe(entity),
                        Math.min(updatedCharge, MAX_CHARGE),
                        MAX_CHARGE,
                        String.format(Locale.ROOT, "%.2f", consumed)
                );
            } else {
                ChestCavity.LOGGER.debug(
                        "{} {} charge -> {}/{}", LOG_PREFIX, describe(entity),
                        Math.min(updatedCharge, MAX_CHARGE), MAX_CHARGE
                );
            }
        }

        if (updatedCharge >= MAX_CHARGE) {
            NBTCharge.setCharge(organ, STATE_KEY, 0);
            NetworkUtil.sendOrganSlotUpdate(chestCavity, organ);
            dispensePrimaryReward(entity);
            maybeGrantBonus(entity, chestCavity);
        } else {
            NBTCharge.setCharge(organ, STATE_KEY, updatedCharge);
            NetworkUtil.sendOrganSlotUpdate(chestCavity, organ);
        }
    }

    /** Ensures linkage channels exist for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance chestCavity) {

        // Linkage channel gating temporarily disabled until a dedicated producer exists.

    }

    private static int clampCharge(int rawCharge) {
        if (rawCharge <= 0) {
            return 0;
        }
        return Math.min(MAX_CHARGE, rawCharge);
    }

    private static boolean hasHealthReserve(LivingEntity entity) {
        float health = entity.getHealth();
        float absorption = Math.max(0.0f, entity.getAbsorptionAmount());
        return (health + absorption) - HEALTH_COST >= MINIMUM_HEALTH_RESERVE;
    }

    private static boolean drainHealth(LivingEntity entity) {
        return GuzhenrenResourceCostHelper.drainHealth(
                entity,
                HEALTH_COST,
                MINIMUM_HEALTH_RESERVE,
                entity.damageSources().generic()
        );
    }


    private static void dispensePrimaryReward(LivingEntity entity) {
        ItemStack reward = new ItemStack(CCItems.GUZHENREN_JIANJITENG);
        if (entity instanceof Player player) {
            if (!player.addItem(reward)) {
                player.drop(reward, false);
                ChestCavity.LOGGER.info("{} {} 完成充能 -> 掉落剑脊藤", LOG_PREFIX, player.getScoreboardName());
            } else {
                ChestCavity.LOGGER.info("{} {} 完成充能 -> 获得剑脊藤", LOG_PREFIX, player.getScoreboardName());
            }
            return;
        }
        entity.spawnAtLocation(reward);
        ChestCavity.LOGGER.info("{} {} 完成充能 -> 掉落剑脊藤", LOG_PREFIX, describe(entity));
    }

    private static void maybeGrantBonus(LivingEntity entity, ChestCavityInstance cc) {
        if (!hasFullStack(cc)) {
            ChestCavity.LOGGER.debug("{} {} 没有满组剑脊藤 -> 不触发额外奖励", LOG_PREFIX, describe(entity));
            return;
        }
        if (entity.getRandom().nextInt(BONUS_ROLL) != 0) {
            ChestCavity.LOGGER.debug("{} {} 随机检定未通过 -> 不触发额外奖励", LOG_PREFIX, describe(entity));
            return;
        }
        Item bonusItem = CCItems.pickRandomGuzhenrenJiandaoBonus(entity.getRandom());
        String bonusId = BuiltInRegistries.ITEM.getKey(bonusItem).toString();
        ItemStack bonus = new ItemStack(bonusItem);
        if (entity instanceof Player player) {
            if (!player.addItem(bonus)) {
                player.drop(bonus, false);
                ChestCavity.LOGGER.info("{} {} 额外奖励 -> 掉落 {}", LOG_PREFIX, player.getScoreboardName(), bonusId);
            } else {
                ChestCavity.LOGGER.info("{} {} 额外奖励 -> 获得 {}", LOG_PREFIX, player.getScoreboardName(), bonusId);
            }
        } else {
            entity.spawnAtLocation(bonus);
            ChestCavity.LOGGER.info("{} {} 额外奖励 -> 掉落 {}", LOG_PREFIX, describe(entity), bonusId);
        }
        celebrateBonus(entity);
    }


    private static boolean hasFullStack(ChestCavityInstance cc) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            // 这里必须是满堆叠 == 64，不能 >=
            if (stack.is(CCItems.GUZHENREN_JIANJITENG) && stack.getCount() == 64) {
                return true;
            }
        }
        return false;
    }

    private static void celebrateBonus(LivingEntity entity) {
        if (entity instanceof Player player) {
            player.displayClientMessage(BONUS_MESSAGE, true);
        }
        playBonusSounds(entity);
        spawnBonusParticles(entity);
    }

    private static void playBonusSounds(LivingEntity entity) {
        var level = entity.level();
        var source = entity.getSoundSource();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, source, 0.8f, 0.9f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, source, 0.35f, 0.6f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BEEHIVE_SHEAR, source, 0.6f, 0.8f);
    }

    private static void spawnBonusParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel server)) {
            return;
        }
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.6;
        double z = entity.getZ();
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.25, 0.3, 0.25, 0.05);
        server.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 6, 0.2, 0.2, 0.2, 0.04);
        server.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 8, 0.18, 0.35, 0.18, 0.08);
        server.sendParticles(ParticleTypes.PORTAL, x, y, z, 16, 0.35, 0.35, 0.35, 0.1);
    }

    private static String describe(LivingEntity entity) {
        return entity.getScoreboardName();
    }

}
