package kizuna.guzhenren_event_ext.common.event.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when a player's inventory gains one or more items of a certain type.
 * This event is fired by the PlayerInventoryWatcher.
 */
public class PlayerObtainedItemEvent extends GuzhenrenPlayerEvent {

    private final Item item;
    private final int count;

    public PlayerObtainedItemEvent(Player player, Item item, int count) {
        super(player);
        this.item = item;
        this.count = count;
    }

    /**
     * @return The type of item that was obtained.
     */
    public Item getItem() {
        return item;
    }

    /**
     * @return The number of items of this type that were added to the inventory.
     */
    public int getCount() {
        return count;
    }

    /**
     * @return An example ItemStack of the obtained item.
     */
    public ItemStack getItemStack() {
        return new ItemStack(item, count);
    }
}
