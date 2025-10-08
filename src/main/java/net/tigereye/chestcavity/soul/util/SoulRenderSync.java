package net.tigereye.chestcavity.soul.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Small network helper to ensure SoulPlayer equipment (armor, main/off-hand) renders on clients.
 */
public final class SoulRenderSync {

    private SoulRenderSync() {}

    /** Broadcasts a full equipment snapshot for the given living entity to all clients. */
    public static void syncEquipment(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            list.add(Pair.of(slot, stack.copy()));
        }
        serverLevel.getServer().getPlayerList().broadcastAll(
                new ClientboundSetEquipmentPacket(entity.getId(), list)
        );
    }

    /**
     * If the player's main hand is empty, pick the first non-empty hotbar slot as selected
     * so the main-hand item can render after inventory restores.
     */
    public static void ensureMainHandSelected(Player player) {
        if (!player.getMainHandItem().isEmpty()) return;
        for (int i = 0; i < 9; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().selected = i;
                break;
            }
        }
    }

    /** Convenience: broadcast equipment without changing selected hotbar. */
    public static void syncEquipmentForPlayer(Player player) {
        syncEquipment(player);
    }
}
