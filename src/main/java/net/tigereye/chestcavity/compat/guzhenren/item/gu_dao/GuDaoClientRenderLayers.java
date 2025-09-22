package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.fml.ModList;

/** Client-side helpers that attach bone dao render layers to player models. */
public final class GuDaoClientRenderLayers {

    private GuDaoClientRenderLayers() {
    }

    public static void register() {
        if (!ModList.get().isLoaded("guzhenren")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.getEntityRenderDispatcher().getSkinMap().forEach((skin, renderer) -> {
            if (renderer instanceof PlayerRenderer playerRenderer) {
                attachLayer(playerRenderer);
            }
        });
    }

    private static void attachLayer(PlayerRenderer renderer) {
        if (renderer == null) {
            return;
        }
        renderer.addLayer(new GuQiangguRenderLayer(renderer));
    }
}
