package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.ui.ChestCavityScreenHandler;

public class CCContainers {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, ChestCavity.MODID);

    public static final RegistryObject<MenuType<ChestCavityScreenHandler>> CHEST_CAVITY_SCREEN_HANDLER = MENU_TYPES.register("chest_cavity_screen", () -> new MenuType<>(ChestCavityScreenHandler::new));

}
