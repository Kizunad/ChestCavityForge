package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Bone Bamboo Gu (骨竹蛊) grows a shared bone_growth linkage channel.
 * Passive: each slow tick adds +5 per organ stack.
 * Active: using bone meal near the player injects +20 per stack.
 */
public enum GuzhuguOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gu_zhu_gu");
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");

    private static final double PASSIVE_GAIN = 5.0;
    private static final double ACTIVE_GAIN = 20.0;
    private static final double SOFT_CAP = 120.0;
    private static final double FALL_OFF = 0.5;
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
    private static final SaturationPolicy SOFT_CAP_POLICY = new SaturationPolicy(SOFT_CAP, FALL_OFF);

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player) || entity.level().isClientSide()) {
            return;
        }
        int count = Math.max(1, organ.getCount());
        LinkageChannel channel = ensureChannel(cc);
        double delta = PASSIVE_GAIN * count;
        double newValue = channel.adjust(delta);
        sendDebugMessage((Player) entity, "PASSIVE", delta, newValue);
    }

    /** Triggered when the player uses bone meal; applies the active boost when off cooldown. */
    public boolean onBoneMealCatalyst(Player player, ChestCavityInstance cc) {
        if (player == null || cc == null || player.level().isClientSide()) {
            return false;
        }
        int totalStacks = countOrgans(cc);
        if (totalStacks <= 0) {
            return false;
        }

        LinkageChannel channel = ensureChannel(cc);
        double delta = ACTIVE_GAIN * totalStacks;
        double newValue = channel.adjust(delta);
        sendDebugMessage(player, "ACTIVE", delta, newValue);
        playCatalystCue(player.level(), player);
        return true;
    }

    /** Prepares the linkage channel for this chest cavity (idempotent). */
    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc);
    }

    public boolean hasOrgan(ChestCavityInstance cc) {
        return countOrgans(cc) > 0;
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.lookupChannel(CHANNEL_ID)
                .orElseGet(() -> context.getOrCreateChannel(CHANNEL_ID)
                        .addPolicy(NON_NEGATIVE)
                        .addPolicy(SOFT_CAP_POLICY));
    }

    private static int countOrgans(ChestCavityInstance cc) {
        int total = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                total += Math.max(1, stack.getCount());
            }
        }
        return total;
    }

    private static void playCatalystCue(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static void sendDebugMessage(Player player, String action, double delta, double newValue) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        Component message = Component.literal(String.format("[Guzhugu] %s +%.1f => %.1f", action, delta, newValue));
        player.sendSystemMessage(message);
        player.displayClientMessage(message, false);
    }
}
