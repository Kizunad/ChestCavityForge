package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordSlashProjectile;


/**
 * Centralised entity type registration for the Chest Cavity mod.
 */
public final class CCEntities {

    private CCEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ChestCavity.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BoneGunProjectile>> BONE_GUN_PROJECTILE =
            ENTITY_TYPES.register("bone_gun_projectile", () -> EntityType.Builder
                    .<BoneGunProjectile>of(BoneGunProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ChestCavity.MODID + ":bone_gun_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SingleSwordProjectile>> SINGLE_SWORD_PROJECTILE =
            ENTITY_TYPES.register("single_sword_projectile", () -> EntityType.Builder
                    .<SingleSwordProjectile>of(SingleSwordProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ChestCavity.MODID + ":single_sword_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwordShadowClone>> SWORD_SHADOW_CLONE =
            ENTITY_TYPES.register("sword_shadow_clone", () -> EntityType.Builder
                    .<SwordShadowClone>of(SwordShadowClone::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(48)
                    .updateInterval(2)
                    .build(ChestCavity.MODID + ":sword_shadow_clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwordSlashProjectile>> SWORD_SLASH_PROJECTILE =
            ENTITY_TYPES.register("sword_slash", () -> EntityType.Builder
                    .<SwordSlashProjectile>of(SwordSlashProjectile::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .build(ChestCavity.MODID + ":sword_slash"));

}
