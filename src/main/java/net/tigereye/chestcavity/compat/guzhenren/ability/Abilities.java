package net.tigereye.chestcavity.compat.guzhenren.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb.BloodBoneBombAbility;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.registration.CCKeybindings;
import net.tigereye.chestcavity.registration.CCOrganScores;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers Guzhenren-specific active abilities exposed through the Chest Cavity hotkeys.
 */
public final class Abilities {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    /** Identifier shared with the Dragon Bombs hotkey so the dedicated binding triggers the ability. */
    public static final ResourceLocation BLOOD_BONE_BOMB = CCOrganScores.DRAGON_BOMBS;

    private Abilities() {
    }

    /**
     * Registers server-side activation handlers. Safe to invoke multiple times.
     */
    public static void bootstrap() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        OrganActivationListeners.register(BLOOD_BONE_BOMB, Abilities::activateBloodBoneBomb);
    }

    /**
     * Hooks client keybinding metadata so the Blood Bone Bomb can be activated from the hotkey.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        CCKeybindings.ATTACK_ABILITY_LIST.removeIf(BLOOD_BONE_BOMB::equals);
    }

    private static void activateBloodBoneBomb(LivingEntity entity, ChestCavityInstance chestCavity) {
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[Guzhenren] Blood Bone Bomb ability triggered for {}", entity);
        }
        BloodBoneBombAbility.tryActivate(entity, chestCavity);
    }
}
