package net.tigereye.chestcavity.soul.runtime;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将附近掉落物缓慢吸向 SoulPlayer，并在接近时触发拾取。
 *
 * 默认关闭，可通过命令或 JVM 选项开启：-Dchestcavity.soul.vacuum.enabled=true
 */
public final class ItemVacuumHandler implements SoulRuntimeHandler {

    private static final AtomicBoolean ENABLED = new AtomicBoolean(Boolean.getBoolean("chestcavity.soul.vacuum.enabled"));
    private static volatile double RADIUS = parseDouble("chestcavity.soul.vacuum.radius", 5.0);
    private static volatile double PULL = parseDouble("chestcavity.soul.vacuum.pull", 0.35);
    private static volatile int INTERVAL = (int) Math.max(1, parseDouble("chestcavity.soul.vacuum.interval", 5.0));
    private static volatile double PICKUP_RANGE = parseDouble("chestcavity.soul.vacuum.pickupRange", 1.25);
    private static final boolean DEBUG = Boolean.getBoolean("chestcavity.debugSoul.vacuum");

    public static void setEnabled(boolean enabled) { ENABLED.set(enabled); }
    public static boolean isEnabled() { return ENABLED.get(); }
    public static void setRadius(double r) { RADIUS = Math.max(0.5, Math.min(24.0, r)); }
    public static double getRadius() { return RADIUS; }

    @Override
    public void onTickEnd(SoulPlayer player) {
        if (!ENABLED.get()) return;
        if (player.level().isClientSide) return;
        // 节流：错峰运行，降低压力
        if ((player.tickCount % INTERVAL) != 0) return;

        Vec3 center = player.position().add(0, 0.35, 0);
        AABB box = new AABB(center, center).inflate(RADIUS);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, box,
                it -> it != null && it.isAlive() && !it.getItem().isEmpty());

        for (ItemEntity it : items) {
            Vec3 delta = center.subtract(it.position());
            double dist = delta.length();
            if (dist < 1.0e-3) {
                it.setNoPickUpDelay();
                it.playerTouch(player);
                continue;
            }
            // 吸附：按固定强度拉近（上限不超过与中心的距离）
            Vec3 pull = delta.normalize().scale(Math.min(PULL, dist));
            it.setDeltaMovement(it.getDeltaMovement().add(pull));

            if (dist <= PICKUP_RANGE) {
                it.setNoPickUpDelay();
                try {
                    it.playerTouch(player);
                } catch (Throwable t) {
                    if (DEBUG) SoulLog.info("[soul][vacuum] playerTouch failed: {}", t.toString());
                }
            }
        }
    }

    private static double parseDouble(String key, double def) {
        try {
            return Double.parseDouble(System.getProperty(key, String.valueOf(def)));
        } catch (Throwable t) {
            return def;
        }
    }
}
