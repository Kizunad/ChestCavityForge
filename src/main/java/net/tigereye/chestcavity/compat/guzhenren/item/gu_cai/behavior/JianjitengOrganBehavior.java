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
    private static final int MAX_CHARGE = 20;

    private static final float HEALTH_COST = 0.5f;
    private static final double ZHENYUAN_COST = 2.0;
    private static final double ENERGY_COST = 5.0;

    private static final ResourceLocation ENERGY_CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jianjiteng_energy");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

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

        LinkageChannel energyChannel = ensureEnergyChannel(chestCavity);
        int currentCharge = clampCharge(NBTCharge.getCharge(organ, STATE_KEY));

        if (!canAffordHealth(player)) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} health below safety threshold", LOG_PREFIX, player.getScoreboardName());
            }
            return;
        }
        if (!hasSufficientEnergy(energyChannel)) {
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug("{} {} linkage energy insufficient", LOG_PREFIX, player.getScoreboardName());
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
        energyChannel.adjust(-ENERGY_COST);

        int updatedCharge = currentCharge + 1;
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            double consumed = zhenyuanResult.getAsDouble();
            ChestCavity.LOGGER.debug(
                    "{} {} charge -> {}/{} (消耗真元 {}, 剩余能量 {})",
                    LOG_PREFIX,
                    player.getScoreboardName(),
                    Math.min(updatedCharge, MAX_CHARGE),
                    MAX_CHARGE,
                    String.format(Locale.ROOT, "%.2f", consumed),
                    String.format(Locale.ROOT, "%.1f", energyChannel.get())
            );
        }

        if (updatedCharge >= MAX_CHARGE) {
            NBTCharge.setCharge(organ, STATE_KEY, 0);
            NetworkUtil.sendOrganSlotUpdate(chestCavity, organ);
            dispensePrimaryReward(player);
            maybeGrantBonus(player);
        } else {
            NBTCharge.setCharge(organ, STATE_KEY, updatedCharge);
            NetworkUtil.sendOrganSlotUpdate(chestCavity, organ);
        }
    }

    /** Ensures linkage channels exist for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance chestCavity) {
        if (chestCavity == null) {
            return;
        }
        ensureEnergyChannel(chestCavity);
    }

    private static LinkageChannel ensureEnergyChannel(ChestCavityInstance chestCavity) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(chestCavity);
        return context.getOrCreateChannel(ENERGY_CHANNEL_ID).addPolicy(NON_NEGATIVE);
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
    }

    private static boolean hasSufficientEnergy(LinkageChannel channel) {
        return channel.get() >= ENERGY_COST;
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

    private static void maybeGrantBonus(Player player) {
        if (!hasFullStack(player)) {
            return;
        }
        if (player.getRandom().nextInt(BONUS_ROLL) != 0) {
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

    private static boolean hasFullStack(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(GuzhenrenItems.JIANJITENG) && stack.getCount() >= 64) {
                return true;
            }
        }
        return false;
    }
}
