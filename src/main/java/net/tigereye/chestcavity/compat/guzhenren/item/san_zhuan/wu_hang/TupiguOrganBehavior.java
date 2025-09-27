package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * Tupigu converts zhenyuan into jingli boosts on a slow cadence with a small jump bonus.
 */
public enum TupiguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener, IncreaseEffectContributor {
    INSTANCE;

    private static final double BASE_COST = 400.0;
    private static final double JINGLI_PER_TRIGGER = 10.0;
    private static final int JUMP_EFFECT_TICKS = 20;
    private static final int JUMP_AMPLIFIER = 0;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // Work handled in onSlowTick to avoid per tick cost.
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || !player.onGround()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            int stackCount = Math.max(1, organ.getCount());
            double totalCost = BASE_COST * stackCount;
            if (handle.consumeScaledZhenyuan(totalCost).isEmpty()) {
                return;
            }

            handle.adjustJingli(JINGLI_PER_TRIGGER * stackCount, true);

            MobEffectInstance current = player.getEffect(MobEffects.JUMP);
            if (current == null || current.getDuration() <= JUMP_EFFECT_TICKS / 2) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_EFFECT_TICKS, JUMP_AMPLIFIER, false, false, true));
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1f, 1.1f);
            }
        });
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        // Tupigu does not contribute to INCREASE effects.
    }
}
