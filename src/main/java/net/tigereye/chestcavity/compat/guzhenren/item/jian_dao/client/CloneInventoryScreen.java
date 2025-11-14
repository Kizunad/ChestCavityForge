package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu;

/**
 * 分身物品栏GUI界面
 *
 * <p>显示7个槽位的分身物品栏（6个蛊虫 + 1个增益）以及玩家背包。
 */
public class CloneInventoryScreen extends AbstractContainerScreen<CloneInventoryMenu> {

    // 使用原版的小容器纹理作为基础
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "textures/gui/clone_inventory.png");

    // 备用：使用原版纹理（如果自定义纹理未准备好）
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/hopper.png");

    /**
     * 构造函数
     *
     * @param menu 菜单处理器
     * @param inventory 玩家背包
     * @param title 界面标题
     */
    public CloneInventoryScreen(CloneInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        // GUI尺寸
        this.imageWidth = 176;   // 标准容器宽度
        this.imageHeight = 166;  // 分身槽位(36) + 玩家背包(76) + 快捷栏(18) + 边距(36)

        // 标签位置
        this.inventoryLabelY = this.imageHeight - 94;  // 玩家背包标签位置
    }

    @Override
    protected void init() {
        super.init();
        // 标题居中
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 尝试使用自定义纹理，如果失败则使用备用纹理
        try {
            renderCustomBackground(graphics, x, y);
        } catch (Exception e) {
            renderFallbackBackground(graphics, x, y);
        }
    }

    /**
     * 渲染自定义背景纹理
     */
    private void renderCustomBackground(GuiGraphics graphics, int x, int y) {
        // 渲染完整的自定义GUI背景
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    /**
     * 渲染备用背景（使用原版纹理拼接）
     */
    private void renderFallbackBackground(GuiGraphics graphics, int x, int y) {
        // 顶部容器区域 (包含分身槽位)
        // 使用原版 hopper 纹理的顶部
        graphics.blit(FALLBACK_TEXTURE, x, y, 0, 0, imageWidth, 70);

        // 玩家背包区域
        // 使用原版容器纹理的底部（背包 + 快捷栏）
        ResourceLocation containerTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
        graphics.blit(containerTexture, x, y + 70, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 渲染背景（暗化效果）
        renderBackground(graphics, mouseX, mouseY, delta);

        // 渲染容器和物品
        super.render(graphics, mouseX, mouseY, delta);

        // 渲染工具提示
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 渲染标题
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        // 渲染玩家背包标签
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        // 可选：添加槽位提示文本
        // 如果需要在特定槽位旁边显示文本（如"蛊虫"、"增益"），可以在这里实现
    }
}
