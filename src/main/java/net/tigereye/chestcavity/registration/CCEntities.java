package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.entity.XiaoGuangIllusionEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.entity.MadBullEntity;
import net.tigereye.chestcavity.entity.SwordSlashProjectile;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile;
import net.tigereye.chestcavity.soul.entity.SoulChunkLoaderEntity;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;
import net.tigereye.chestcavity.soul.entity.TestSoulEntity;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostEntity;

/** Centralised entity type registration for the Chest Cavity mod. */
public final class CCEntities {

  private CCEntities() {}

  public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
      DeferredRegister.create(Registries.ENTITY_TYPE, ChestCavity.MODID);

  public static final DeferredHolder<EntityType<?>, EntityType<BoneGunProjectile>>
      BONE_GUN_PROJECTILE =
          ENTITY_TYPES.register(
              "bone_gun_projectile",
              () ->
                  EntityType.Builder.<BoneGunProjectile>of(BoneGunProjectile::new, MobCategory.MISC)
                      .sized(0.5f, 0.5f)
                      .clientTrackingRange(64)
                      .updateInterval(1)
                      .build(ChestCavity.MODID + ":bone_gun_projectile"));

  public static final DeferredHolder<EntityType<?>, EntityType<SingleSwordProjectile>>
      SINGLE_SWORD_PROJECTILE =
          ENTITY_TYPES.register(
              "single_sword_projectile",
              () ->
                  EntityType.Builder.<SingleSwordProjectile>of(
                          SingleSwordProjectile::new, MobCategory.MISC)
                      .sized(0.5f, 0.5f)
                      .clientTrackingRange(64)
                      .updateInterval(1)
                      .build(ChestCavity.MODID + ":single_sword_projectile"));

  public static final DeferredHolder<EntityType<?>, EntityType<SwordShadowClone>>
      SWORD_SHADOW_CLONE =
          ENTITY_TYPES.register(
              "sword_shadow_clone",
              () ->
                  EntityType.Builder.<SwordShadowClone>of(SwordShadowClone::new, MobCategory.MISC)
                      .sized(0.6f, 1.8f)
                      .clientTrackingRange(48)
                      .updateInterval(2)
                      .build(ChestCavity.MODID + ":sword_shadow_clone"));

  public static final DeferredHolder<EntityType<?>, EntityType<XiaoGuangIllusionEntity>>
      XIAO_GUANG_ILLUSION =
          ENTITY_TYPES.register(
              "xiao_guang_illusion",
              () ->
                  EntityType.Builder.<XiaoGuangIllusionEntity>of(
                          XiaoGuangIllusionEntity::new, MobCategory.MISC)
                      .sized(0.6f, 1.8f)
                      .clientTrackingRange(48)
                      .updateInterval(2)
                      .build(ChestCavity.MODID + ":xiao_guang_illusion"));

  public static final DeferredHolder<EntityType<?>, EntityType<SwordSlashProjectile>> SWORD_SLASH =
      ENTITY_TYPES.register(
          "sword_slash",
          () ->
              EntityType.Builder.<SwordSlashProjectile>of(
                      SwordSlashProjectile::new, MobCategory.MISC)
                  .sized(0.6f, 0.6f)
                  .clientTrackingRange(64)
                  .updateInterval(1)
                  .build(ChestCavity.MODID + ":sword_slash"));

  public static final DeferredHolder<EntityType<?>, EntityType<MadBullEntity>> MAD_BULL =
      ENTITY_TYPES.register(
          "mad_bull",
          () ->
              EntityType.Builder.<MadBullEntity>of(MadBullEntity::new, MobCategory.MISC)
                  .sized(0.9f, 0.9f)
                  .clientTrackingRange(48)
                  .updateInterval(1)
                  .build(ChestCavity.MODID + ":mad_bull"));

  public static final DeferredHolder<EntityType<?>, EntityType<SoulChunkLoaderEntity>>
      SOUL_CHUNK_LOADER =
          ENTITY_TYPES.register(
              "soul_chunk_loader",
              () ->
                  EntityType.Builder.<SoulChunkLoaderEntity>of(
                          SoulChunkLoaderEntity::new, MobCategory.MISC)
                      .sized(0.1f, 0.1f)
                      .clientTrackingRange(16)
                      .updateInterval(20)
                      .build(ChestCavity.MODID + ":soul_chunk_loader"));

  public static final DeferredHolder<EntityType<?>, EntityType<TestSoulEntity>> TEST_SOUL =
      ENTITY_TYPES.register(
          "test_soul",
          () ->
              EntityType.Builder.<TestSoulEntity>of(TestSoulEntity::new, MobCategory.CREATURE)
                  .sized(0.6f, 1.8f)
                  .clientTrackingRange(48)
                  .updateInterval(2)
                  .build(ChestCavity.MODID + ":test_soul"));

  public static final DeferredHolder<EntityType<?>, EntityType<SoulClanEntity>> SOUL_CLAN =
      ENTITY_TYPES.register(
          "soul_clan",
          () ->
              EntityType.Builder.<SoulClanEntity>of(SoulClanEntity::new, MobCategory.CREATURE)
                  .sized(0.6f, 1.8f)
                  .clientTrackingRange(48)
                  .updateInterval(2)
                  .build(ChestCavity.MODID + ":soul_clan"));

  public static final DeferredHolder<EntityType<?>, EntityType<PlayerGhostEntity>> PLAYER_GHOST =
      ENTITY_TYPES.register(
          "player_ghost",
          () ->
              EntityType.Builder.<PlayerGhostEntity>of(
                      PlayerGhostEntity::new, MobCategory.MONSTER)
                  .sized(0.6f, 1.8f)
                  .clientTrackingRange(64)
                  .updateInterval(2)
                  .build(ChestCavity.MODID + ":player_ghost"));

  public static final DeferredHolder<EntityType<?>, EntityType<FlyingSwordEntity>> FLYING_SWORD =
      ENTITY_TYPES.register(
          "flying_sword",
          () ->
              EntityType.Builder.<FlyingSwordEntity>of(FlyingSwordEntity::new, MobCategory.MISC)
                  .sized(0.5f, 0.5f)
                  .clientTrackingRange(64)
                  .updateInterval(1)
                  .build(ChestCavity.MODID + ":flying_sword"));

  // 与 FLYING_SWORD 逻辑相同的“正道飞剑”，用于数据驱动的特殊初始器官
  public static final DeferredHolder<EntityType<?>, EntityType<FlyingSwordEntity>>
      FLYING_SWORD_ZHENG_DAO =
          ENTITY_TYPES.register(
              "flying_sword_zheng_dao",
              () ->
                  EntityType.Builder.<FlyingSwordEntity>of(
                          FlyingSwordEntity::new, MobCategory.MISC)
                      .sized(0.5f, 0.5f)
                      .clientTrackingRange(64)
                      .updateInterval(1)
                      .build(ChestCavity.MODID + ":flying_sword_zheng_dao"));

  // 与 FLYING_SWORD 逻辑相同的"人兽葬生"飞剑（拼音），用于数据驱动的特殊初始器官
  public static final DeferredHolder<EntityType<?>, EntityType<FlyingSwordEntity>>
      FLYING_SWORD_REN_SHOU_ZANG_SHENG =
          ENTITY_TYPES.register(
              "flying_sword_ren_shou_zang_sheng",
              () ->
                  EntityType.Builder.<FlyingSwordEntity>of(
                          FlyingSwordEntity::new, MobCategory.MISC)
                      .sized(0.5f, 0.5f)
                      .clientTrackingRange(64)
                      .updateInterval(1)
                      .build(ChestCavity.MODID + ":flying_sword_ren_shou_zang_sheng"));

  // 裂剑蛊：裂隙实体
  public static final DeferredHolder<
          EntityType<?>,
          EntityType<net.tigereye.chestcavity.compat.guzhenren.rift.RiftEntity>>
      RIFT =
          ENTITY_TYPES.register(
              "rift",
              () ->
                  EntityType.Builder
                      .<net.tigereye.chestcavity.compat.guzhenren.rift.RiftEntity>of(
                          net.tigereye.chestcavity.compat.guzhenren.rift.RiftEntity::new,
                          MobCategory.MISC)
                      .sized(2.5f, 1.6f) // 主裂隙尺寸（最大）
                      .clientTrackingRange(64)
                      .updateInterval(20) // 每秒同步
                      .noSave() // 不保存到区块（临时实体）
                      .fireImmune()
                      .build(ChestCavity.MODID + ":rift"));
}
