package net.tigereye.chestcavity.soulbeast;

import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastRuntimeEvents {

    private SoulBeastRuntimeEvents() {}

    public static void onMeleeHit(AttackEntityEvent event) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents.onMeleeHit(event);
    }

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents.onProjectileImpact(event);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents.onServerTick(event);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents.onIncomingDamage(event);
    }

    public static void onSoulBeastStateChanged(SoulBeastStateChangedEvent event) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.SoulBeastRuntimeEvents.onSoulBeastStateChanged(event);
    }
}
