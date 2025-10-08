package net.tigereye.chestcavity.client.modernui.container;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared helpers for the Modern UI test container to avoid duplication between
 * commands and network handlers.
 */
public final class TestModernUIContainerDebug {

    private static final Component TITLE = Component.literal("Modern UI Container Test");

    private TestModernUIContainerDebug() {}

    public static AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        var container = TestModernUIContainerMenu.createSeededContainer();
        return new TestModernUIContainerMenu(syncId, inventory, container);
    }

    public static SimpleMenuProvider provider() {
        return new SimpleMenuProvider(TestModernUIContainerDebug::createMenu, TITLE);
    }

    public static void openFor(ServerPlayer player) {
        player.openMenu(provider());
    }

    static void seedDefaultItems(net.minecraft.world.Container container) {
        container.setItem(0, new ItemStack(Items.APPLE));
        container.setItem(1, new ItemStack(Items.DIAMOND));
        container.setItem(2, new ItemStack(Items.GOLDEN_PICKAXE));
        container.setItem(3, new ItemStack(Items.SPYGLASS));
        container.setItem(4, new ItemStack(Items.WRITABLE_BOOK));
        container.setItem(5, new ItemStack(Items.ENDER_EYE));
    }
}
