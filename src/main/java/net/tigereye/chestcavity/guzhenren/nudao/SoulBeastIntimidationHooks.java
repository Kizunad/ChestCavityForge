package net.tigereye.chestcavity.guzhenren.nudao;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soulbeast.state.event.SoulBeastStateChangedEvent;

/**
 * Tracks which players currently satisfy the soul beast intimidation aura.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastIntimidationHooks {

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    private SoulBeastIntimidationHooks() {
    }

    @SubscribeEvent
    public static void onSoulBeastStateChanged(SoulBeastStateChangedEvent event) {
        if (!(event.entity() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUUID();
        if (event.current().isSoulBeast()) {
            ACTIVE.add(uuid);
        } else {
            ACTIVE.remove(uuid);
        }
    }

    public static boolean isIntimidationEnabled(Player player) {
        return player != null && ACTIVE.contains(player.getUUID());
    }
}
