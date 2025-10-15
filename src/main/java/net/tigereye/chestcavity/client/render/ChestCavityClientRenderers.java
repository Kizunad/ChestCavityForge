package net.tigereye.chestcavity.client.render;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * Registers general-purpose client renderers for Chest Cavity entities.
 */
public final class ChestCavityClientRenderers {

    private ChestCavityClientRenderers() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CCEntities.SWORD_SLASH.get(), SwordSlashRenderer::new);
        // 显式渲染“发疯的牛”冲锋体（MadBullEntity）
        event.registerEntityRenderer(CCEntities.MAD_BULL.get(),
                ctx -> new net.tigereye.chestcavity.compat.guzhenren.item.li_dao.client.MadBullRenderer(ctx));
    }
}
