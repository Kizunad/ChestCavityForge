package net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Shared FX identifiers used by the Blood Bone Bomb ability and its projectile.
 */
public final class BloodBoneBombFx {

    public static final ResourceLocation CHARGE_START = ChestCavity.id("blood_bone_charge_start");
    public static final ResourceLocation CHARGE_TICK = ChestCavity.id("blood_bone_charge_tick");
    public static final ResourceLocation CHARGE_HEARTBEAT = ChestCavity.id("blood_bone_charge_heartbeat");
    public static final ResourceLocation CHARGE_FAILURE = ChestCavity.id("blood_bone_charge_failure");
    public static final ResourceLocation PROJECTILE_LAUNCH = ChestCavity.id("blood_bone_projectile_launch");
    public static final ResourceLocation PROJECTILE_IMPACT = ChestCavity.id("blood_bone_projectile_impact");
    public static final ResourceLocation PROJECTILE_FADE = ChestCavity.id("blood_bone_projectile_fade");

    private BloodBoneBombFx() {
    }
}
