package net.tigereye.chestcavity.soul.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 通用掉落物吸取逻辑，便于在不同实体上共享。
 */
public final class ItemAbsorptionOps {

    private ItemAbsorptionOps() {
    }

    public static void pullAndProcess(Entity entity,
                                      double radius,
                                      double pullStrength,
                                      Predicate<ItemEntity> filter,
                                      Consumer<ItemEntity> onPickup) {
        if (entity == null || !entity.isAlive()) {
            return;
        }
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }
        Vec3 center = entity.position().add(0, 0.35, 0);
        AABB box = new AABB(center, center).inflate(radius);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                it -> it != null && it.isAlive() && !it.getItem().isEmpty());
        for (ItemEntity item : items) {
            if (filter != null && !filter.test(item)) {
                continue;
            }
            Vec3 delta = center.subtract(item.position());
            double dist = delta.length();
            if (dist < 1.0e-3) {
                onPickup.accept(item);
                continue;
            }
            Vec3 pull = delta.normalize().scale(Math.min(pullStrength, dist));
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
            if (dist <= 1.25) {
                onPickup.accept(item);
            }
        }
    }

    public static Predicate<ItemEntity> notNullAnd(Predicate<ItemEntity> delegate) {
        return item -> item != null && delegate.test(item);
    }

    public static Predicate<ItemEntity> byStack(Predicate<ItemEntity> base, Predicate<ItemEntity> extra) {
        return item -> base.test(item) && extra.test(item);
    }
}
