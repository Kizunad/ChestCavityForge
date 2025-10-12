package net.tigereye.chestcavity.soul.navigation;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulNavigationEvents {
    private SoulNavigationEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SoulNavigationMirror.serverTick(event.getServer());
        SoulNavigationTestHarness.tick(event.getServer());
    }
}
