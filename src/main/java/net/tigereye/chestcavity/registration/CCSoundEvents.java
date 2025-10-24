package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;

/** Holds mod {@link SoundEvent} registrations. */
public final class CCSoundEvents {

  private CCSoundEvents() {}

  public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
      DeferredRegister.create(Registries.SOUND_EVENT, ChestCavity.MODID);

  public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SWORD_BREAK_AIR =
      SOUND_EVENTS.register(
          "custom.sword.break_air",
          () -> SoundEvent.createVariableRangeEvent(ChestCavity.id("custom.sword.break_air")));

  public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SOULBEAST_FAIL_TO_SOULBEAST =
      SOUND_EVENTS.register(
          "custom.soulbeast.fail_to_soulbeast",
          () ->
              SoundEvent.createVariableRangeEvent(
                  ChestCavity.id("custom.soulbeast.fail_to_soulbeast")));

  public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_SOULBEAST_DOT =
      SOUND_EVENTS.register(
          "custom.soulbeast.dot",
          () -> SoundEvent.createVariableRangeEvent(ChestCavity.id("custom.soulbeast.dot")));

  public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_FIRE_HUO_YI =
      SOUND_EVENTS.register(
          "custom.fire.huo_yi",
          () -> SoundEvent.createVariableRangeEvent(ChestCavity.id("custom.fire.huo_yi")));

  public static final DeferredHolder<SoundEvent, SoundEvent> CUSTOM_FIGHT_PUNCH =
      SOUND_EVENTS.register(
          "custom.fight.punch",
          () -> SoundEvent.createVariableRangeEvent(ChestCavity.id("custom.fight.punch")));
}
