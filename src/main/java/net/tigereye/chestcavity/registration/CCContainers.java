package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.container.TestModernUIContainerMenu;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu;
import net.tigereye.chestcavity.guscript.ui.GuScriptMenu;
import net.tigereye.chestcavity.ui.ChestCavityScreenHandler;

public class CCContainers {
  public static final DeferredRegister<MenuType<?>> MENU_TYPES =
      DeferredRegister.create(Registries.MENU, ChestCavity.MODID);

  public static final DeferredHolder<MenuType<?>, MenuType<ChestCavityScreenHandler>>
      CHEST_CAVITY_SCREEN_HANDLER =
          MENU_TYPES.register(
              "chest_cavity_screen",
              () -> new MenuType<>(ChestCavityScreenHandler::new, FeatureFlags.VANILLA_SET));
  public static final DeferredHolder<MenuType<?>, MenuType<GuScriptMenu>> GUSCRIPT_MENU =
      MENU_TYPES.register(
          "guscript_menu", () -> new MenuType<>(GuScriptMenu::new, FeatureFlags.VANILLA_SET));
  public static final DeferredHolder<MenuType<?>, MenuType<TestModernUIContainerMenu>>
      TEST_MODERN_UI_MENU =
          MENU_TYPES.register(
              "test_modernui_menu",
              () -> new MenuType<>(TestModernUIContainerMenu::new, FeatureFlags.VANILLA_SET));
  public static final DeferredHolder<MenuType<?>, MenuType<CloneInventoryMenu>>
      CLONE_INVENTORY_MENU =
          MENU_TYPES.register(
              "clone_inventory_menu",
              () -> new MenuType<>(CloneInventoryMenu::new, FeatureFlags.VANILLA_SET));
}
