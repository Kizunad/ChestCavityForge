package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Holds mod {@link SoundEvent} registrations.
 */
public final class CCSoundEvents {

    private CCSoundEvents() {
    }

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ChestCavity.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SWORD_BREAK_AIR =
            SOUND_EVENTS.register("custom.sword.break_air",
                    () -> SoundEvent.createVariableRangeEvent(ChestCavity.id("custom.sword.break_air")));
}
