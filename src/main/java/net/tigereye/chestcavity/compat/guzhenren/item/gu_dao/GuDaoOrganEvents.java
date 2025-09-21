package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * Handles player interactions relevant to 骨道蛊 organs.
 */
final class GuDaoOrganEvents {

    private static boolean registered;

    private GuDaoOrganEvents() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(GuDaoOrganEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(GuDaoOrganEvents::onRightClickBlock);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) {
            return;
        }
        handleBoneMeal(event.getLevel(), event.getEntity(), event.getItemStack());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }
        handleBoneMeal(event.getLevel(), event.getEntity(), event.getItemStack());
    }

    private static void handleBoneMeal(Level level, Player player, ItemStack stack) {
        if (level.isClientSide() || stack.isEmpty() || player == null) {
            return;
        }
        if (!ModList.get().isLoaded("guzhenren")) {
            return;
        }
        if (!stack.is(Items.BONE_MEAL)) {
            return;
        }
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
            if (!cc.opened) {
                return;
            }
            GuzhuguOrganBehavior.INSTANCE.onBoneMealCatalyst(player, cc);
        });
    }
}
