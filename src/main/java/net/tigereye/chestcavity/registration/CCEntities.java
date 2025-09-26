package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb.BoneGunProjectile;
import net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb.SingleSwordProjectile;

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
}
