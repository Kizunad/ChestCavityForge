package net.tigereye.chestcavity.compat.guzhenren.ability;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities} instead.
 */
@Deprecated(forRemoval = false)
public final class Abilities {

    private Abilities() {
    }

    /** Identifier shared with the Dragon Bombs hotkey so the dedicated binding triggers the ability. */
    public static final ResourceLocation BLOOD_BONE_BOMB =
            net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities.BLOOD_BONE_BOMB;

    /**
     * Registers server-side activation handlers. Safe to invoke multiple times.
     */
    public static void bootstrap() {
        net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities.bootstrap();
    }

    /**
     * Hooks client keybinding metadata so the Blood Bone Bomb can be activated from the hotkey.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities.onClientSetup(event);
    }
}
