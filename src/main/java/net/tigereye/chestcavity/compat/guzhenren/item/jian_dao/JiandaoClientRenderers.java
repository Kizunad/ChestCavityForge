package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.JianQiGuSlashRenderer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.PersistentGuCultivatorCloneRenderer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.SingleSwordProjectileRenderer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client.SwordShadowCloneRenderer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.FlyingSwordRenderer;
import net.tigereye.chestcavity.compat.guzhenren.rift.client.RiftRenderer;
import net.tigereye.chestcavity.registration.CCEntities;

/** Registers client-only renderers related to the sword shadow organ set. */
public final class JiandaoClientRenderers {

  private JiandaoClientRenderers() {}

  public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(
        CCEntities.SINGLE_SWORD_PROJECTILE.get(), SingleSwordProjectileRenderer::new);
    event.registerEntityRenderer(
        CCEntities.SWORD_SHADOW_CLONE.get(), SwordShadowCloneRenderer::new);
    event.registerEntityRenderer(
        CCEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
    event.registerEntityRenderer(
        CCEntities.FLYING_SWORD_ZHENG_DAO.get(), FlyingSwordRenderer::new);
    event.registerEntityRenderer(
        CCEntities.FLYING_SWORD_REN_SHOU_ZANG_SHENG.get(), FlyingSwordRenderer::new);
    // 裂剑蛊：裂隙渲染器
    event.registerEntityRenderer(CCEntities.RIFT.get(), RiftRenderer::new);
    // 剑气蛊：剑光斩击渲染器
    event.registerEntityRenderer(CCEntities.JIAN_QI_GU_SLASH.get(), JianQiGuSlashRenderer::new);
    // 多重剑影蛊：持久化蛊修分身渲染器
    event.registerEntityRenderer(
        CCEntities.PERSISTENT_GU_CULTIVATOR_CLONE.get(),
        PersistentGuCultivatorCloneRenderer::new);
  }
}
