package net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile} instead.
 */
@Deprecated(forRemoval = false)
public class BoneGunProjectile extends net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile {

    @SuppressWarnings("unchecked")
    public BoneGunProjectile(EntityType<? extends BoneGunProjectile> type, Level level) {
        super((EntityType<? extends net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BoneGunProjectile>) (EntityType<?>) type, level);
    }

    public BoneGunProjectile(Level level, LivingEntity shooter, ItemStack stack) {
        super(level, shooter, stack);
    }
}
