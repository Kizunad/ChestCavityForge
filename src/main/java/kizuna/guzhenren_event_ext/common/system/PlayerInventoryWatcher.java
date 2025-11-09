package kizuna.guzhenren_event_ext.common.system;

import kizuna.guzhenren_event_ext.common.config.Gamerules;
import kizuna.guzhenren_event_ext.common.event.CustomEventBus;
import kizuna.guzhenren_event_ext.common.event.api.PlayerObtainedItemEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInventoryWatcher {

    private static final PlayerInventoryWatcher INSTANCE = new PlayerInventoryWatcher();

    private final Map<UUID, Map<Item, Integer>> lastKnownItemCounts = new ConcurrentHashMap<>();
    private boolean active = false;
    private int pollingInterval = 1200; // 1 minute (20 ticks/sec * 60 sec)
    private int tickCounter = 0;

    private PlayerInventoryWatcher() {}

    public static PlayerInventoryWatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Activates the watcher. This should be called by the EventLoader if any 'player_obtained_item' triggers are found.
     */
    public void activate() {
        if (!this.active) {
            this.active = true;
            this.lastKnownItemCounts.clear(); // Clear cache on activation
        }
    }

    public void setPollingInterval(int intervalInTicks) {
        this.pollingInterval = Math.max(20, intervalInTicks); // Minimum 1 second
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!active) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (!server.getGameRules().getBoolean(Gamerules.RULE_EVENT_EXTENSION_ENABLED)) {
            return;
        }

        tickCounter++;
        if (tickCounter < pollingInterval) {
            return;
        }
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayerInventory(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (active && !event.getEntity().level().isClientSide()) {
            this.lastKnownItemCounts.remove(event.getEntity().getUUID());
        }
    }

    private void checkPlayerInventory(ServerPlayer player) {
        Map<Item, Integer> currentItemCounts = new HashMap<>();
        // Iterate over the main inventory
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                currentItemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        // Also check offhand inventory
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty()) {
                currentItemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        Map<Item, Integer> oldItemCounts = lastKnownItemCounts.computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        // Find new or increased items
        for (Map.Entry<Item, Integer> entry : currentItemCounts.entrySet()) {
            Item item = entry.getKey();
            int newCount = entry.getValue();
            int oldCount = oldItemCounts.getOrDefault(item, 0);

            if (newCount > oldCount) {
                int delta = newCount - oldCount;
                CustomEventBus.EVENT_BUS.post(new PlayerObtainedItemEvent(player, item, delta));
            }
        }

        // Update the cache with the new counts
        lastKnownItemCounts.put(player.getUUID(), currentItemCounts);
    }
}
