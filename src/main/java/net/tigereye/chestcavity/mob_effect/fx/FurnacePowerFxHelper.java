package net.tigereye.chestcavity.mob_effect.fx;


import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralises the lightweight state-to-sound linkage for the Furnace Power
 * effect. The helper keeps the gameplay code clean and makes the mapping
 * easily testable.
 */
public final class FurnacePowerFxHelper {

    public static final int CHARGE_SOUND_INTERVAL = 40;

    private static final Map<FurnaceFlowState, SoundEvent> STATE_SOUNDS =
            new EnumMap<>(FurnaceFlowState.class);

    static {
        STATE_SOUNDS.put(FurnaceFlowState.CHARGING, SoundEvents.FURNACE_FIRE_CRACKLE);
        STATE_SOUNDS.put(FurnaceFlowState.FEEDING, SoundEvents.PLAYER_BURP);
    }

    private FurnacePowerFxHelper() {
    }

    /**
     * Returns the sound effect tied to the supplied flow state.
     */
    public static Optional<SoundEvent> soundFor(FurnaceFlowState state) {
        return Optional.ofNullable(STATE_SOUNDS.get(state));
    }

    /**
     * Plays the sound effect that corresponds with the supplied flow state.
     */
    public static void playFx(LivingEntity entity, FurnaceFlowState state) {
        if (entity == null) {
            return;
        }
        soundFor(state).ifPresent(sound -> entity.playSound(sound, 0.75F, 1.0F));
    }
}
