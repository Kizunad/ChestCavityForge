package net.tigereye.chestcavity.registration;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.ui.ChestCavityScreenHandler;

public class CCContainers {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, ChestCavity.MODID);

    public static final RegistryObject<MenuType<ChestCavityScreenHandler>> CHEST_CAVITY_SCREEN_HANDLER = MENU_TYPES.register("chest_cavity_screen", () -> new MenuType<>(ChestCavityScreenHandler::new));

}
