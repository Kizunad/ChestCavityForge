package net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.behavior;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.item.GuzhenrenItems;

import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;

import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NBTCharge;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Locale;

/**
 * Behaviour for 剑脊藤 organ stacks. Gradually charges by draining player resources.
 */
public enum JianjitengOrganBehavior implements OrganSlowTickListener {
    INSTANCE;


    private static final String MOD_ID = "guzhenren";

    private static final String STATE_KEY = "JianjitengCharge";
    private static final int MAX_CHARGE = 30;

    private static final float HEALTH_COST = 0.1f;
    private static final double ZHENYUAN_COST = 2.0;

    // TODO: Reintroduce Jianjiteng linkage energy consumption once a producer exists.


    private static final int BONUS_ROLL = 100;
    private static final String LOG_PREFIX = "[Jianjiteng]";

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance chestCavity, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (chestCavity == null) {
            return;
        }


        int currentCharge = clampCharge(NBTCharge.getCharge(organ, STATE_KEY));

        if (!canAffordHealth(player)) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} health below safety threshold", LOG_PREFIX, player.getScoreboardName());
            }
            return;
        }


        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} unable to open resource bridge", LOG_PREFIX, player.getScoreboardName());
            }
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        OptionalDouble zhenyuanResult = handle.consumeScaledZhenyuan(ZHENYUAN_COST);
        if (zhenyuanResult.isEmpty()) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} lacks zhenyuan for charging", LOG_PREFIX, player.getScoreboardName());
            }
            return;
        }

        drainHealth(player);


        int updatedCharge = currentCharge + 1;
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            double consumed = zhenyuanResult.getAsDouble();
            ChestCavity.LOGGER.debug(

                    "{} {} charge -> {}/{} (消耗真元 {})",

                    LOG_PREFIX,
                    player.getScoreboardName(),
                    Math.min(updatedCharge, MAX_CHARGE),
                    MAX_CHARGE,

                    String.format(Locale.ROOT, "%.2f", consumed)

            );
        }

        if (updatedCharge >= MAX_CHARGE) {
            NBTCharge.setCharge(organ, STATE_KEY, 0);
            NetworkUtil.sendOrganSlotUpdate(chestCavity, organ);
            dispensePrimaryReward(player);
            maybeGrantBonus(player, chestCavity);
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

    private static boolean canAffordHealth(Player player) {
        return player.getHealth() > 1.0f;
    }

    private static void drainHealth(Player player) {
        float current = player.getHealth();
        float updated = Math.max(1.0f, current - HEALTH_COST);
        player.setHealth(updated);
        player.hurtTime = 0; 
        player.hurtDuration = 0;
        player.hurtMarked = false; 
    }


    private static void dispensePrimaryReward(Player player) {
        ItemStack reward = new ItemStack(GuzhenrenItems.JIANJITENG);
        if (!player.addItem(reward)) {
            player.drop(reward, false);
            ChestCavity.LOGGER.info("{} {} 完成充能 -> 掉落剑脊藤", LOG_PREFIX, player.getScoreboardName());
        } else {
            ChestCavity.LOGGER.info("{} {} 完成充能 -> 获得剑脊藤", LOG_PREFIX, player.getScoreboardName());
        }
    }

    private static void maybeGrantBonus(Player player, ChestCavityInstance cc) {
        if (!hasFullStack(player, cc)) {
            ChestCavity.LOGGER.debug("{} {} 没有满组剑脊藤 -> 不触发额外奖励", LOG_PREFIX, player.getScoreboardName());
            return;
        }
        if (player.getRandom().nextInt(BONUS_ROLL) != 0) {
            ChestCavity.LOGGER.debug("{} {} 随机检定未通过 -> 不触发额外奖励", LOG_PREFIX, player.getScoreboardName());
            return;
        }
        Item bonusItem = GuzhenrenItems.pickRandomJiandaoBonus(player.getRandom());
        String bonusId = BuiltInRegistries.ITEM.getKey(bonusItem).toString();
        ItemStack bonus = new ItemStack(bonusItem);
        if (!player.addItem(bonus)) {
            player.drop(bonus, false);
            ChestCavity.LOGGER.info("{} {} 额外奖励 -> 掉落 {}", LOG_PREFIX, player.getScoreboardName(), bonusId);
        } else {
            ChestCavity.LOGGER.info("{} {} 额外奖励 -> 获得 {}", LOG_PREFIX, player.getScoreboardName(), bonusId);
        }
    }


    private static boolean hasFullStack(Player player, ChestCavityInstance cc) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            // 这里必须是满堆叠 == 64，不能 >=
            if (stack.is(GuzhenrenItems.JIANJITENG) && stack.getCount() == 64) {
                return true;
            }
        }
        return false;
    }

}
