package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.ChestCavity;

@EventBusSubscriber(modid = ChestCavity.MODID, value = Dist.CLIENT)
public final class GuDaoClientRenderLayers {

    private GuDaoClientRenderLayers() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new GuQiangguRenderLayer(renderer));
            }
        }
    }
}
