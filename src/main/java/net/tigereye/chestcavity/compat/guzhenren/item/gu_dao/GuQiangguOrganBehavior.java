package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Gu Qiang Gu (骨枪蛊) converts stored bone_growth energy into a brief strength boost.
 */
public enum GuQiangguOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double COST_PER_STACK = 10.0;
    private static final int EFFECT_DURATION_TICKS = 20 * 3; // 3 seconds
    private static final int EFFECT_AMPLIFIER = 0; // Strength I

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        int stacks = Math.max(1, organ.getCount());
        LinkageChannel channel = ensureChannel(cc);
        double available = channel.get();
        double cost = COST_PER_STACK * stacks;
        if (available < cost) {
            return;
        }

        channel.adjust(-cost);
        applyBuff(player, stacks);
        sendFeedback(player, cost, channel.get());
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureChannel(cc);
    }

    private static LinkageChannel ensureChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.configureChannel(CHANNEL_ID, channel -> channel.addPolicy(NON_NEGATIVE));
    }

    private static void applyBuff(Player player, int stacks) {
        int amplifier = Math.min(3, EFFECT_AMPLIFIER + stacks - 1);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, amplifier, false, true, true));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.5f, 1.1f);
    }

    private static void sendFeedback(Player player, double consumed, double remaining) {
        Component message = Component.literal(String.format("[GuQianggu] -%.1f bone_growth => %.1f", consumed, remaining));
        player.sendSystemMessage(message);
        player.displayClientMessage(message, false);
    }
}
