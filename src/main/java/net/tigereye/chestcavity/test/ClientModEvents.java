package net.tigereye.chestcavity.test;


import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.ChestCavity;

@Mod.EventBusSubscriber(modid = ChestCavity.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        //MenuScreens.register(BlockEntityRegistry.CONT.get(), TestScreen::new);
        MenuScreens.register(BlockEntityRegistry.EXAMPLE_CHEST_MENU.get(), ExampleChestScreen::new);
    }
}