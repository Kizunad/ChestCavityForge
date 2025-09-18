package net.tigereye.chestcavity.registration;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.mob_effect.CCStatusEffect;
import net.tigereye.chestcavity.mob_effect.FurnacePower;
import net.tigereye.chestcavity.mob_effect.OrganRejection;
import net.tigereye.chestcavity.mob_effect.Ruminating;

public class CCStatusEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ChestCavity.MODID);

    public static final RegistryObject<MobEffect> ORGAN_REJECTION = MOB_EFFECTS.register("organ_rejection", OrganRejection::new);
    public static final RegistryObject<MobEffect> ARROW_DODGE_COOLDOWN = MOB_EFFECTS.register("arrow_dodge_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> DRAGON_BOMB_COOLDOWN = MOB_EFFECTS.register("dragon_bomb_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> DRAGON_BREATH_COOLDOWN = MOB_EFFECTS.register("dragon_breath_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> EXPLOSION_COOLDOWN = MOB_EFFECTS.register("explosion_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> FORCEFUL_SPIT_COOLDOWN = MOB_EFFECTS.register("forceful_spit_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> FURNACE_POWER = MOB_EFFECTS.register("furnace_power", FurnacePower::new);
    public static final RegistryObject<MobEffect> GHASTLY_COOLDOWN = MOB_EFFECTS.register("ghastly_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> IRON_REPAIR_COOLDOWN = MOB_EFFECTS.register("iron_repair_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> PYROMANCY_COOLDOWN = MOB_EFFECTS.register("pyromancy_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> RUMINATING = MOB_EFFECTS.register("ruminating", Ruminating::new);
    public static final RegistryObject<MobEffect> SHULKER_BULLET_COOLDOWN = MOB_EFFECTS.register("shulker_bullet_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> SILK_COOLDOWN = MOB_EFFECTS.register("silk_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> VENOM_COOLDOWN = MOB_EFFECTS.register("venom_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final RegistryObject<MobEffect> WATER_VULNERABILITY = MOB_EFFECTS.register("water_vulnerability", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
}
