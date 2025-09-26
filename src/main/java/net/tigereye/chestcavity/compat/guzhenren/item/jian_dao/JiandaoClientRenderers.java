package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.SingleSwordProjectileRenderer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.SwordShadowCloneRenderer;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * Registers client-only renderers related to the sword shadow organ set.
 */
public final class JiandaoClientRenderers {

    private JiandaoClientRenderers() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CCEntities.SINGLE_SWORD_PROJECTILE.get(), SingleSwordProjectileRenderer::new);
        event.registerEntityRenderer(CCEntities.SWORD_SHADOW_CLONE.get(), SwordShadowCloneRenderer::new);
    }
}

