package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.mob_effect.CCStatusEffect;
import net.tigereye.chestcavity.mob_effect.FurnacePower;
import net.tigereye.chestcavity.mob_effect.OrganRejection;
import net.tigereye.chestcavity.mob_effect.Ruminating;

public class CCStatusEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ChestCavity.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> ORGAN_REJECTION = MOB_EFFECTS.register("organ_rejection", OrganRejection::new);
    public static final DeferredHolder<MobEffect, MobEffect> ARROW_DODGE_COOLDOWN = MOB_EFFECTS.register("arrow_dodge_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> DRAGON_BOMB_COOLDOWN = MOB_EFFECTS.register("dragon_bomb_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> DRAGON_BREATH_COOLDOWN = MOB_EFFECTS.register("dragon_breath_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> EXPLOSION_COOLDOWN = MOB_EFFECTS.register("explosion_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> FORCEFUL_SPIT_COOLDOWN = MOB_EFFECTS.register("forceful_spit_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> FURNACE_POWER = MOB_EFFECTS.register("furnace_power", FurnacePower::new);
    public static final DeferredHolder<MobEffect, MobEffect> GHASTLY_COOLDOWN = MOB_EFFECTS.register("ghastly_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> IRON_REPAIR_COOLDOWN = MOB_EFFECTS.register("iron_repair_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> PYROMANCY_COOLDOWN = MOB_EFFECTS.register("pyromancy_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> RUMINATING = MOB_EFFECTS.register("ruminating", Ruminating::new);
    public static final DeferredHolder<MobEffect, MobEffect> SHULKER_BULLET_COOLDOWN = MOB_EFFECTS.register("shulker_bullet_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> SILK_COOLDOWN = MOB_EFFECTS.register("silk_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> VENOM_COOLDOWN = MOB_EFFECTS.register("venom_cooldown", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
    public static final DeferredHolder<MobEffect, MobEffect> WATER_VULNERABILITY = MOB_EFFECTS.register("water_vulnerability", () -> new CCStatusEffect(MobEffectCategory.NEUTRAL,0x000000));
}
