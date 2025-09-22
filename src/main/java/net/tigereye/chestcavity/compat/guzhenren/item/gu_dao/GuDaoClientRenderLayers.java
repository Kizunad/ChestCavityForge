package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.render.GuQiangguRenderLayer;

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

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        // 直接调用复用的渲染逻辑
    }
}
