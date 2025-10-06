package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guards a ServerPlayer against incoming damage during sensitive switch phases
 * (teleport + capability apply + base restore).
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulSwitchGuard {

    private static final Map<UUID, Integer> GUARD = new ConcurrentHashMap<>();

    private SoulSwitchGuard() {}

    public static void begin(ServerPlayer player, String reason) {
        UUID id = player.getUUID();
        GUARD.merge(id, 1, Integer::sum);
        SoulLog.info("[soul] switch-guard begin owner={} reason={}", id, reason);
    }

    public static void end(ServerPlayer player, String reason) {
        UUID id = player.getUUID();
        GUARD.computeIfPresent(id, (k, v) -> v > 1 ? v - 1 : null);
        SoulLog.info("[soul] switch-guard end owner={} reason={}", id, reason);
    }

    public static boolean isGuarded(ServerPlayer player) {
        return GUARD.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isGuarded(player)) return;
        // Cancel any damage while guarded (prevents OUT_OF_WORLD style kills from compat during switch)
        event.setCanceled(true);
        SoulLog.info("[soul] switch-guard canceled damage owner={} type={} amount={}",
                player.getUUID(), event.getSource().typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown"), event.getAmount());
    }
}

