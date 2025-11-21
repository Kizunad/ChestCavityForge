package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.registration.CCEntities;

/** Helper for spawning Hun Dao soul avatars using data-driven templates. */
public final class HunDaoSoulAvatarSpawner {

  private HunDaoSoulAvatarSpawner() {}

  public static Optional<HunDaoSoulAvatarEntity> spawn(
      ServerLevel level, ServerPlayer owner, Vec3 position, ResourceLocation templateId) {
    if (level == null || owner == null) {
      return Optional.empty();
    }
    HunDaoSoulAvatarTemplate template = HunDaoSoulAvatarTemplates.get(templateId);
    EntityType<HunDaoSoulAvatarEntity> type = CCEntities.HUN_DAO_SOUL_AVATAR.get();
    HunDaoSoulAvatarEntity avatar = type.create(level);
    if (avatar == null) {
      return Optional.empty();
    }
    avatar.moveTo(position.x, position.y, position.z, owner.getYRot(), owner.getXRot());
    avatar.setTemplateId(template.id());
    avatar.setLifetimeTicks(template.lifetimeTicks());
    avatar.initialiseFromOwner(owner);
    avatar.setSkin(captureSkin(owner, template));
    level.addFreshEntity(avatar);
    return Optional.of(avatar);
  }

  private static PlayerSkinUtil.SkinSnapshot captureSkin(
      ServerPlayer owner, HunDaoSoulAvatarTemplate template) {
    PlayerSkinUtil.SkinSnapshot base =
        template.captureSkin() ? PlayerSkinUtil.capture(owner) : null;
    return PlayerSkinUtil.withTint(base, template.tintR(), template.tintG(), template.tintB(), template.tintAlpha());
  }
}
