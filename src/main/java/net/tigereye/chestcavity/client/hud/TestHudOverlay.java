package net.tigereye.chestcavity.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.ui.HudUiPaint;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ChestCavity.MODID, value = Dist.CLIENT)
public class TestHudOverlay {
  private static volatile boolean enabled = false;
  private static volatile ResourceLocation icon =
      ResourceLocation.parse("modernui:textures/item/project_builder.png");
  private static volatile net.minecraft.world.item.ItemStack itemIcon =
      net.minecraft.world.item.ItemStack.EMPTY;

  public static void setEnabled(boolean value) {
    enabled = value;
  }

  public static void setIcon(ResourceLocation texture) {
    if (texture != null) icon = texture;
  }

  public static void setItemIcon(net.minecraft.world.item.ItemStack stack) {
    itemIcon = stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack;
  }

  @SubscribeEvent
  public static void onRenderHud(RenderGuiEvent.Post event) {
    if (!enabled && !net.tigereye.chestcavity.client.ui.ModernUiClientState.showActionHud()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.options.hideGui) return;

    GuiGraphics g = event.getGuiGraphics();
    int pad = 4;
    int x = mc.getWindow().getGuiScaledWidth() - 120;
    int y = 8;
    int w = 112;
    int h = 28;

    HudUiPaint.drawCard(g, x, y, w, h);
    if (!itemIcon.isEmpty()) {
      HudUiPaint.drawItem24(g, itemIcon, x + pad, y + 2);
    } else {
      HudUiPaint.drawIcon24(g, icon, x + pad, y + 2);
    }
    g.drawString(mc.font, "HUI: on", x + pad + 26, y + 6, HudUiPaint.TEXT_TITLE, false);
    g.drawString(mc.font, "PNG demo", x + pad + 26, y + 16, HudUiPaint.TEXT_SUB, false);
  }
}
