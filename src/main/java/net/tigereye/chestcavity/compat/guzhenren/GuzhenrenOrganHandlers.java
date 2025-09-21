package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnFireContext;
import net.tigereye.chestcavity.listeners.OrganOnFireListener;
import net.neoforged.fml.ModList;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

import java.util.OptionalDouble;

/**
 * Compatibility helpers that inject Guzhenren-specific organ behaviour without direct class dependencies.
 */
public final class GuzhenrenOrganHandlers {

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUOXINGU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu");

    private static final double HUOXINGU_COST_PER_TICK = 5.0;
    private static final float HUOXINGU_HEAL_PER_TICK = 1.0f;

    private static final OrganOnFireListener HUOXINGU_FIRE_LISTENER = (entity, cc, organ) -> {
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
            OptionalDouble zhenyuanOpt = handle.readDouble("zhenyuan");
            if (zhenyuanOpt.isEmpty()) {
                return;
            }
            double available = zhenyuanOpt.getAsDouble();
            int stackCount = Math.max(1, organ.getCount());
            double cost = HUOXINGU_COST_PER_TICK * stackCount;
            if (available < cost) {
                return;
            }

            double newValue = available - cost;
            if (handle.writeDouble("zhenyuan", newValue).isEmpty()) {
                return;
            }

            float healAmount = HUOXINGU_HEAL_PER_TICK * stackCount;
            player.heal(healAmount);
        });
    };

    private GuzhenrenOrganHandlers() {
    }

    static {
        OrganRetentionRules.registerNamespace(MOD_ID);
    }

    public static void registerListeners(ChestCavityInstance cc, ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        if (isHuoxingu(stack)) {
            cc.onFireListeners.add(new OrganOnFireContext(stack, HUOXINGU_FIRE_LISTENER));
        }
    }

    private static boolean isHuoxingu(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.equals(HUOXINGU_ID);
    }
}
