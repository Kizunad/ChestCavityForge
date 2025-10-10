package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Thin wrappers around Guzhenren resource helpers to centralize common patterns.
 */
public final class ResourceOps {

    private ResourceOps() {}

    /** Consume zhenyuan/jingli strictly from a real player; no HP fallback. */
    public static GuzhenrenResourceCostHelper.ConsumptionResult consumeStrict(Player player, double zhenyuan, double jingli) {
        if (player == null) throw new IllegalArgumentException("player is null");
        return GuzhenrenResourceCostHelper.consumeStrict(player, zhenyuan, jingli);
    }

    /** Consume zhenyuan/jingli from any living entity; falls back to HP when not a player. */
    public static GuzhenrenResourceCostHelper.ConsumptionResult consumeWithFallback(LivingEntity entity, double zhenyuan, double jingli) {
        if (entity == null) throw new IllegalArgumentException("entity is null");
        return GuzhenrenResourceCostHelper.consumeWithFallback(entity, zhenyuan, jingli);
    }

    /** Consume zhenyuan/jingli strictly from any entity (players via attachment; no HP fallback). */
    public static GuzhenrenResourceCostHelper.ConsumptionResult consumeStrict(LivingEntity entity, double zhenyuan, double jingli) {
        if (entity == null) throw new IllegalArgumentException("entity is null");
        return GuzhenrenResourceCostHelper.consumeStrict(entity, zhenyuan, jingli);
    }

    /** Consume hunpo strictly (no HP fallback). */
    public static GuzhenrenResourceCostHelper.ConsumptionResult consumeHunpoStrict(LivingEntity entity, double hunpo) {
        if (entity == null) throw new IllegalArgumentException("entity is null");
        return GuzhenrenResourceCostHelper.consumeHunpo(entity, hunpo, false);
    }

    /** Consume hunpo with HP fallback for non-players. */
    public static GuzhenrenResourceCostHelper.ConsumptionResult consumeHunpoWithFallback(LivingEntity entity, double hunpo) {
        if (entity == null) throw new IllegalArgumentException("entity is null");
        return GuzhenrenResourceCostHelper.consumeHunpo(entity, hunpo, true);
    }


    public static OptionalDouble tryAdjustJingli(ResourceHandle handle, double amount, boolean clamp) {
        if (handle == null || amount == 0.0) {
            return OptionalDouble.empty();
        }
        return handle.adjustJingli(amount, clamp);
    }

    public static OptionalDouble tryAdjustJingli(ResourceHandle handle, double amount) {
        return tryAdjustJingli(handle, amount, true);
    }

    public static OptionalDouble tryAdjustJingli(Player player, double amount, boolean clamp) {
        if (player == null || amount == 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryAdjustJingli(handleOpt.get(), amount, clamp);
    }

    public static OptionalDouble tryAdjustJingli(Player player, double amount) {
        return tryAdjustJingli(player, amount, true);
    }

    public static OptionalDouble tryAdjustJingli(LivingEntity entity, double amount, boolean clamp) {
        if (entity == null || amount == 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryAdjustJingli(handleOpt.get(), amount, clamp);
    }

    public static OptionalDouble tryAdjustJingli(LivingEntity entity, double amount) {
        return tryAdjustJingli(entity, amount, true);
    }

    /** Adjust player jingli without needing the return value. */
    public static void adjustJingli(Player player, double amount) {
        tryAdjustJingli(player, amount, true);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(ResourceHandle handle, double amount, boolean clampToMax) {
        if (handle == null || amount <= 0.0) {
            return OptionalDouble.empty();
        }
        return handle.replenishScaledZhenyuan(amount, clampToMax);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(ResourceHandle handle, double amount) {
        return tryReplenishScaledZhenyuan(handle, amount, true);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(Player player, double amount, boolean clampToMax) {
        if (player == null || amount <= 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryReplenishScaledZhenyuan(handleOpt.get(), amount, clampToMax);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(Player player, double amount) {
        return tryReplenishScaledZhenyuan(player, amount, true);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(LivingEntity entity, double amount, boolean clampToMax) {
        if (entity == null || amount <= 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryReplenishScaledZhenyuan(handleOpt.get(), amount, clampToMax);
    }

    public static OptionalDouble tryReplenishScaledZhenyuan(LivingEntity entity, double amount) {
        return tryReplenishScaledZhenyuan(entity, amount, true);
    }

    /** Convenience wrapper retaining legacy behaviour. */
    public static void replenishScaledZhenyuan(Player player, double amount) {
        tryReplenishScaledZhenyuan(player, amount, true);
    }

    public static OptionalDouble tryConsumeScaledZhenyuan(ResourceHandle handle, double baseCost) {
        if (handle == null || baseCost <= 0.0) {
            return OptionalDouble.empty();
        }
        return handle.consumeScaledZhenyuan(baseCost);
    }

    public static OptionalDouble tryConsumeScaledZhenyuan(Player player, double baseCost) {
        if (player == null || baseCost <= 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryConsumeScaledZhenyuan(handleOpt.get(), baseCost);
    }

    public static OptionalDouble tryConsumeScaledZhenyuan(LivingEntity entity, double baseCost) {
        if (entity == null || baseCost <= 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryConsumeScaledZhenyuan(handleOpt.get(), baseCost);
    }

    public static OptionalDouble tryAdjustZhenyuan(ResourceHandle handle, double amount, boolean clampToMax) {
        if (handle == null || amount == 0.0) {
            return OptionalDouble.empty();
        }
        return handle.adjustZhenyuan(amount, clampToMax);
    }

    public static OptionalDouble tryAdjustZhenyuan(Player player, double amount, boolean clampToMax) {
        if (player == null || amount == 0.0) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryAdjustZhenyuan(handleOpt.get(), amount, clampToMax);
    }


    public static OptionalDouble trySetJingli(ResourceHandle handle, double value) {
        if (handle == null) {
            return OptionalDouble.empty();
        }
        return handle.setJingli(value);
    }

    public static OptionalDouble trySetJingli(Player player, double value) {
        if (player == null) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return trySetJingli(handleOpt.get(), value);
    }

    public static OptionalDouble tryAdjustDouble(ResourceHandle handle, String field, double amount, boolean clampToMax, String maxField) {
        if (handle == null || field == null) {
            return OptionalDouble.empty();
        }
        return handle.adjustDouble(field, amount, clampToMax, maxField);
    }

    public static OptionalDouble tryAdjustDouble(Player player, String field, double amount, boolean clampToMax, String maxField) {
        if (player == null || field == null) {
            return OptionalDouble.empty();
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return OptionalDouble.empty();
        }
        return tryAdjustDouble(handleOpt.get(), field, amount, clampToMax, maxField);
    }

}