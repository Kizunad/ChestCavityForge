package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.render.GuQiangguRenderLayer;

/** 渲染层注册（Mod Bus 上注册，通过 ChestCavity 主类 addListener 绑定） */
public final class GuDaoClientRenderLayers {
  private GuDaoClientRenderLayers() {}

  // ⚠️ 这里不能再用 @SubscribeEvent（这是 Mod Bus 事件，主类构造函数里 addListener 注册）
  public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
    for (PlayerSkin.Model skin : event.getSkins()) {
      PlayerRenderer renderer = event.getSkin(skin);
      if (renderer != null) {
        renderer.addLayer(new GuQiangguRenderLayer(renderer));
      }
    }
  }
}

/** 专门处理 Client Bus 事件（可以用注解） */
@EventBusSubscriber(modid = ChestCavity.MODID, value = Dist.CLIENT)
final class GuDaoHandRenderEvents {
  private GuDaoHandRenderEvents() {}

  @SubscribeEvent
  public static void onRenderHand(RenderHandEvent event) {
    PoseStack poseStack = event.getPoseStack();
    MultiBufferSource buffer = event.getMultiBufferSource();
    int packedLight = event.getPackedLight();

    // TODO: 调用骨枪的渲染逻辑
  }
}
