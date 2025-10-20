package net.tigereye.chestcavity.client.modernui.widget;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import icyllis.modernui.mc.MinecraftSurfaceView;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

/**
 * Minimal ModernUI slot view that renders a slot background, an item stack and exposes
 * click-based selection feedback. Intended as a prototype for ModernUI skill slots.
 */
public final class SimpleSkillSlotView extends LinearLayout {

    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.parse("minecraft:textures/gui/container/slot.png");

    private final ResourceLocation entryId;
    private final ItemStack stack;
    private final String displayName;
    private final SlotSurfaceRenderer renderer;
    private final MinecraftSurfaceView surface;
    private final SelectionListener selectionListener;
    private boolean selected;

    public SimpleSkillSlotView(@NonNull icyllis.modernui.core.Context context,
                               ResourceLocation entryId,
                               ItemStack iconStack,
                               String displayName,
                               TextView statusView,
                               int sizePx,
                               SelectionListener selectionListener) {
        super(context);
        this.entryId = Objects.requireNonNull(entryId, "entryId");
        this.stack = normalize(iconStack);
        this.displayName = displayName != null && !displayName.isBlank()
                ? displayName
                : this.stack.getHoverName().getString();
        this.renderer = new SlotSurfaceRenderer(sizePx);
        this.surface = new MinecraftSurfaceView(context);
        this.selectionListener = selectionListener;

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        surface.setRenderer(renderer);
        var surfaceParams = new LayoutParams(sizePx, sizePx, Gravity.CENTER_HORIZONTAL);
        addView(surface, surfaceParams);

        var label = new TextView(context);
        label.setText(this.displayName);
        label.setTextSize(11);
        label.setTextColor(0xFFE6F3FF);
        label.setGravity(Gravity.CENTER_HORIZONTAL);
        label.setSingleLine(false);
        label.setMaxLines(2);
        label.setPadding(0, label.dp(4), 0, 0);
        addView(label, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> {
            if (statusView != null) {
                statusView.setText("选中：" + displayName + " (id=" + entryId + ")");
            }
            if (selectionListener != null) {
                selectionListener.onSlotSelected(SimpleSkillSlotView.this);
            }
        });
    }

    public ResourceLocation getEntryId() {
        return entryId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) {
            return;
        }
        this.selected = selected;
        surface.invalidate();
    }

    private ItemStack normalize(ItemStack icon) {
        ItemStack stack = (icon == null || icon.isEmpty()) ? new ItemStack(Items.STONE) : icon.copy();
        stack.setCount(1);
        return stack;
    }

    private final class SlotSurfaceRenderer implements MinecraftSurfaceView.Renderer {
        private int width;
        private int height;

        private SlotSurfaceRenderer(int sizePx) {
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void onDraw(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float deltaTick,
                           double guiScale, float alpha) {
            int guiWidth = (int) Math.round(width / guiScale);
            int guiHeight = (int) Math.round(height / guiScale);

            graphics.fill(0, 0, guiWidth, guiHeight, 0xFF1D2B3B);
            graphics.blit(SLOT_TEXTURE, 0, 0, 0, 0, guiWidth, guiHeight, guiWidth, guiHeight);

            if (selected) {
                int color = 0xAA6F7A8A;
                int th = 2; // 上下边框加粗 2px
                // Top / Bottom thicker
                graphics.fill(0, 0, guiWidth, th, color);
                graphics.fill(0, guiHeight - th, guiWidth, guiHeight, color);
                // Left / Right 保持 1px
                graphics.fill(0, 0, 1, guiHeight, color);
                graphics.fill(guiWidth - 1, 0, guiWidth, guiHeight, color);
            }

            if (!stack.isEmpty()) {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                int itemX = guiWidth / 2 - 8;
                int itemY = guiHeight / 2 - 8;
                graphics.renderItem(stack, itemX, itemY);
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
            }
        }
    }

    @FunctionalInterface
    public interface SelectionListener {
        void onSlotSelected(SimpleSkillSlotView view);
    }
}
