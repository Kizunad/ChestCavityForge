package net.tigereye.chestcavity.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ReminderToast implements Toast {
  private static final int WIDTH = 160;
  private static final int HEIGHT = 32;

  private final Component title;
  private final Component message;
  private final ResourceLocation texture;
  private final ItemStack itemIcon;
  private final long durationMs;
  private long firstDrawTime = -1L;

  public static void show(String title, String message, ResourceLocation texture) {
    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.getToasts() != null) {
      mc.getToasts()
          .addToast(
              new ReminderToast(
                  Component.literal(title),
                  Component.literal(message),
                  texture,
                  ItemStack.EMPTY,
                  3000L));
    }
  }

  public static void showItem(String title, String message, ItemStack item) {
    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.getToasts() != null) {
      mc.getToasts()
          .addToast(
              new ReminderToast(
                  Component.literal(title), Component.literal(message), null, item, 3000L));
    }
  }

  public ReminderToast(
      Component title,
      Component message,
      ResourceLocation texture,
      ItemStack itemIcon,
      long durationMs) {
    this.title = title;
    this.message = message;
    this.texture = texture;
    this.itemIcon = itemIcon == null ? ItemStack.EMPTY : itemIcon;
    this.durationMs = durationMs;
  }

  @Override
  public Visibility render(GuiGraphics graphics, ToastComponent component, long timeMs) {
    if (firstDrawTime < 0L) {
      firstDrawTime = timeMs;
    }

    // Unified paint: shadowed card + icon + text + countdown bar
    HudUiPaint.drawCard(graphics, 0, 0, WIDTH, HEIGHT);
    // Countdown progress under content: draw before icon/text so toast covers the bar
    long elapsed = Math.max(0L, timeMs - firstDrawTime);
    float remain = 1.0f - (durationMs > 0 ? (float) elapsed / (float) durationMs : 1.0f);
    int barPadX = 6; // horizontal inset only
    int barX = barPadX;
    int barW = WIDTH - barPadX * 2;
    int barHBase = 4; // base bar height
    int barHVisible = Math.max(1, (int) Math.floor(barHBase * 0.25f)); // show bottom 1/4
    int barY = HEIGHT - barHVisible; // anchor to bottom edge
    HudUiPaint.drawProgressBar(graphics, barX, barY, barW, barHVisible, remain);

    // Foreground content covers the progress bar
    if (!itemIcon.isEmpty()) {
      HudUiPaint.drawItem24(graphics, itemIcon, 6, 4);
    } else {
      HudUiPaint.drawIcon24(graphics, texture, 6, 4);
    }
    graphics.drawString(component.getMinecraft().font, title, 36, 7, HudUiPaint.TEXT_TITLE, false);
    graphics.drawString(component.getMinecraft().font, message, 36, 18, HudUiPaint.TEXT_SUB, false);

    return (timeMs - firstDrawTime) >= durationMs ? Visibility.HIDE : Visibility.SHOW;
  }

  @Override
  public int width() {
    return WIDTH;
  }

  @Override
  public int height() {
    return HEIGHT;
  }
}
