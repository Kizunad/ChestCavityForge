package net.tigereye.chestcavity.client.event;

import icyllis.modernui.mc.neoforge.MenuScreenFactory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.tigereye.chestcavity.client.modernui.container.TestModernUIContainerFragment;
import net.tigereye.chestcavity.client.modernui.container.TestModernUIContainerMenu;
import net.tigereye.chestcavity.guscript.ui.GuScriptScreen;
import net.tigereye.chestcavity.registration.CCContainers;
import net.tigereye.chestcavity.ui.ChestCavityScreen;

public final class ChestCavityClientEvents {

  private ChestCavityClientEvents() {}

  @SubscribeEvent
  public static void registerMenuScreens(RegisterMenuScreensEvent event) {
    event.register(CCContainers.CHEST_CAVITY_SCREEN_HANDLER.get(), ChestCavityScreen::new);
    event.register(CCContainers.GUSCRIPT_MENU.get(), GuScriptScreen::new);
    event.register(
        CCContainers.TEST_MODERN_UI_MENU.get(),
        MenuScreenFactory.create(
            menu -> new TestModernUIContainerFragment((TestModernUIContainerMenu) menu)));
  }
}
