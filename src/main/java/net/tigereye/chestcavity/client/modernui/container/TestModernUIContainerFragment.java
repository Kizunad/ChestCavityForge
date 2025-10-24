package net.tigereye.chestcavity.client.modernui.container;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Fragment that renders a Modern UI surface bound to a {@link TestModernUIContainerMenu}. Uses
 * individual {@link MinecraftSurfaceView} instances per slot to bridge item rendering while
 * delegating item transfer logic to the underlying container menu.
 */
public class TestModernUIContainerFragment extends Fragment {

  private static final int SLOT_SIZE_DP = 34;
  private static final int SLOT_MARGIN_DP = 3;

  private final TestModernUIContainerMenu menu;

  public TestModernUIContainerFragment(TestModernUIContainerMenu menu) {
    this.menu = menu;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable DataSet savedInstanceState) {
    var context = requireContext();

    var root = new FrameLayout(context);
    root.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    var column = new LinearLayout(context);
    column.setOrientation(LinearLayout.VERTICAL);
    column.setGravity(Gravity.CENTER_HORIZONTAL);
    int padding = root.dp(18);
    column.setPadding(padding, padding, padding, padding);
    column.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER));

    var title = new TextView(context);
    title.setText("Modern UI Container Bridge");
    title.setTextSize(18);
    title.setTextColor(0xFFF0F0F0);
    title.setGravity(Gravity.CENTER_HORIZONTAL);
    column.addView(
        title,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    var subtitle = new TextView(context);
    subtitle.setText(
        "Drag items between grids to verify Modern UI + menu sync.\nShift-click for quick move.");
    subtitle.setTextSize(12);
    subtitle.setTextColor(0xFF9FA7B3);
    subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
    subtitle.setPadding(0, root.dp(8), 0, root.dp(12));
    column.addView(
        subtitle,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    column.addView(
        buildStorageGrid(root),
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    addSpacer(column, root.dp(12));

    column.addView(
        buildPlayerInventoryGrid(root),
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    addSpacer(column, root.dp(6));

    column.addView(
        buildHotbarRow(root),
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    var hint = new TextView(context);
    hint.setText("Press ESC to close. Right-click is mapped via long press.");
    hint.setTextSize(11);
    hint.setTextColor(0xFF708090);
    hint.setGravity(Gravity.CENTER_HORIZONTAL);
    hint.setPadding(0, root.dp(10), 0, 0);
    column.addView(
        hint,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    root.addView(column);

    var cursorOverlay = new CursorItemSurface(context);
    root.addView(
        cursorOverlay,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    // GUI 弹窗（ModernUI）暂时搁置，后续按需求恢复

    return root;
  }

  private void addSpacer(LinearLayout parent, int heightPx) {
    var spacer = new View(requireContext());
    spacer.setLayoutParams(
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
    parent.addView(spacer);
  }

  private LinearLayout buildStorageGrid(FrameLayout root) {
    return createGrid(
        root,
        0,
        TestModernUIContainerMenu.STORAGE_SLOT_COUNT,
        TestModernUIContainerMenu.STORAGE_COLUMNS);
  }

  private LinearLayout buildPlayerInventoryGrid(FrameLayout root) {
    int start = TestModernUIContainerMenu.STORAGE_SLOT_COUNT;
    int count = 27;
    return createGrid(root, start, count, 9);
  }

  private LinearLayout buildHotbarRow(FrameLayout root) {
    int start = TestModernUIContainerMenu.STORAGE_SLOT_COUNT + 27;
    int count = 9;
    return createGrid(root, start, count, 9);
  }

  private LinearLayout createGrid(FrameLayout root, int startIndex, int count, int columns) {
    var context = requireContext();
    var grid = new LinearLayout(context);
    grid.setOrientation(LinearLayout.VERTICAL);
    grid.setGravity(Gravity.CENTER_HORIZONTAL);

    int rows = (int) Math.ceil(count / (double) columns);
    int slotSize = root.dp(SLOT_SIZE_DP);
    int slotMargin = root.dp(SLOT_MARGIN_DP);

    for (int row = 0; row < rows; row++) {
      var rowLayout = new LinearLayout(context);
      rowLayout.setOrientation(LinearLayout.HORIZONTAL);
      rowLayout.setGravity(Gravity.CENTER_HORIZONTAL);

      for (int col = 0; col < columns; col++) {
        int slotIdx = row * columns + col;
        if (slotIdx >= count) {
          break;
        }
        Slot slot = menu.slots.get(startIndex + slotIdx);
        var slotView = new SlotView(context, slot, slotSize);
        var params = new LinearLayout.LayoutParams(slotSize, slotSize);
        params.setMargins(slotMargin, slotMargin, slotMargin, slotMargin);
        rowLayout.addView(slotView, params);
      }

      grid.addView(
          rowLayout,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    return grid;
  }

  private void handleSlotInteraction(Slot slot, int button, boolean quickMove) {
    Minecraft minecraft = Minecraft.getInstance();
    Player clientPlayer = minecraft.player;
    if (clientPlayer == null || minecraft.gameMode == null) {
      return;
    }
    ClickType type = quickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP;
    minecraft.gameMode.handleInventoryMouseClick(
        menu.containerId, slot.index, button, type, clientPlayer);
  }

  // 弹窗类移除

  private void handleKeyInteraction(int keyCode) {
    // Map keyboard keys to vanilla container operations
    if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
      Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null));
    }
  }

  private final class SlotView extends FrameLayout {
    private final Slot slot;
    private final SlotRenderer renderer;

    SlotView(@NonNull icyllis.modernui.core.Context context, Slot slot, int sizePx) {
      super(context);
      this.slot = slot;
      setClickable(true);
      setFocusable(true);

      var surface = new MinecraftSurfaceView(context);
      renderer = new SlotRenderer(slot);
      surface.setRenderer(renderer);
      addView(surface, new LayoutParams(sizePx, sizePx, Gravity.CENTER));

      setOnClickListener(
          v -> {
            boolean quickMove =
                Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.isShiftKeyDown();
            handleSlotInteraction(slot, 0, quickMove);
          });

      setOnLongClickListener(
          v -> {
            handleSlotInteraction(slot, 1, false);
            return true;
          });
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
        handleKeyInteraction(keyCode);
        return true;
      }
      return super.onKeyDown(keyCode, event);
    }
  }

  private static final class SlotRenderer implements MinecraftSurfaceView.Renderer {
    private final Slot slot;
    private int surfaceWidth;
    private int surfaceHeight;

    private SlotRenderer(Slot slot) {
      this.slot = slot;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
      surfaceWidth = width;
      surfaceHeight = height;
    }

    @Override
    public void onDraw(
        @NonNull GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float deltaTick,
        double guiScale,
        float alpha) {
      int guiWidth = (int) Math.round(surfaceWidth / guiScale);
      int guiHeight = (int) Math.round(surfaceHeight / guiScale);

      graphics.fill(0, 0, guiWidth, guiHeight, 0xFF1C1F26);
      graphics.fill(1, 1, guiWidth - 1, guiHeight - 1, 0xFF2B323D);

      boolean hovered = mouseX >= 0 && mouseX < guiWidth && mouseY >= 0 && mouseY < guiHeight;
      if (hovered) {
        graphics.fill(1, 1, guiWidth - 1, guiHeight - 1, 0x334A90E2);
      }

      ItemStack stack = slot.getItem();
      if (!stack.isEmpty()) {
        int itemX = guiWidth / 2 - 8;
        int itemY = guiHeight / 2 - 8;
        graphics.renderItem(stack, itemX, itemY);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
      }
    }
  }

  private final class CursorItemSurface extends MinecraftSurfaceView {
    CursorItemSurface(@NonNull icyllis.modernui.core.Context context) {
      super(context);
      setRenderer(new CursorRenderer());
      setWillNotDraw(true);
    }

    private final class CursorRenderer implements MinecraftSurfaceView.Renderer {

      @Override
      public void onSurfaceChanged(int width, int height) {
        // no-op
      }

      @Override
      public void onDraw(
          @NonNull GuiGraphics graphics,
          int mouseX,
          int mouseY,
          float deltaTick,
          double guiScale,
          float alpha) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
          return;
        }
        int itemX = mouseX - 8;
        int itemY = mouseY - 8;
        graphics.renderItem(carried, itemX, itemY);
        graphics.renderItemDecorations(Minecraft.getInstance().font, carried, itemX, itemY);
      }
    }
  }
}
