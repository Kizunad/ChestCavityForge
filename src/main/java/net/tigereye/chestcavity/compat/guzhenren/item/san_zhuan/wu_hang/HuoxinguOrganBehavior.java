package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.listeners.OrganOnFireListener;

/**
 * Handles slow-burning recovery for the Guzhenren Huoxingu organ.
 */
public enum HuoxinguOrganBehavior implements OrganOnFireListener, IncreaseEffectContributor {
    INSTANCE;

    private static final double BASE_COST = 100.0;
    private static final float HEAL_PER_TICK = 0.5f;

    @Override
    public void onFireTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }
        if (!entity.isOnFire() || player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            int stackCount = Math.max(1, organ.getCount());
            double totalCost = BASE_COST * stackCount;
            if (handle.consumeScaledZhenyuan(totalCost).isEmpty()) {
                return;
            }
            float healAmount = HEAL_PER_TICK * stackCount;
            if (healAmount > 0f) {
                player.heal(healAmount);
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
        // Huoxingu does not contribute to INCREASE effects.
    }
}
