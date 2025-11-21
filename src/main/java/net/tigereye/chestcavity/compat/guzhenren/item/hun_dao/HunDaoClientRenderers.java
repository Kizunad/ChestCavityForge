package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoSoulAvatarRenderer;
import net.tigereye.chestcavity.registration.CCEntities;

/** Registers Hun Dao specific entity renderers. */
public final class HunDaoClientRenderers {

  private HunDaoClientRenderers() {}

  public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(
        CCEntities.HUN_DAO_SOUL_AVATAR.get(), HunDaoSoulAvatarRenderer::new);
    event.registerEntityRenderer(
        CCEntities.HUN_DAO_SOUL_AVATAR_BOSS.get(), HunDaoSoulAvatarRenderer::new);
  }
}
