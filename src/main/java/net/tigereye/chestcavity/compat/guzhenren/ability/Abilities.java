package net.tigereye.chestcavity.compat.guzhenren.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/**
 * Registers Guzhenren-specific active abilities exposed through the Chest Cavity hotkeys.
 */
public final class Abilities {

    private static final String MOD_ID = "guzhenren";

    /** Identifier for the combined Blood/Bone offensive ability. */
    public static final ResourceLocation BLOOD_BONE_BOMB =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "blood_bone_bomb");

    static {
        OrganActivationListeners.register(BLOOD_BONE_BOMB, Abilities::activateBloodBoneBomb);
    }

    private Abilities() {
    }

    /**
     * Forces class initialisation when invoked from bootstrap hooks.
     */
    public static void bootstrap() {
        // no-op
    }

    private static void activateBloodBoneBomb(LivingEntity entity, ChestCavityInstance chestCavity) {
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[Guzhenren] Blood Bone Bomb ability triggered for {}", entity);
        }
    }
}
