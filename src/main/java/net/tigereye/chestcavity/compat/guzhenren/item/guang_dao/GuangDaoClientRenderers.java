package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.client.XiaoGuangIllusionRenderer;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * 注册光道相关实体渲染器。
 */
public final class GuangDaoClientRenderers {

    private GuangDaoClientRenderers() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CCEntities.XIAO_GUANG_ILLUSION.get(), XiaoGuangIllusionRenderer::new);
    }
}
