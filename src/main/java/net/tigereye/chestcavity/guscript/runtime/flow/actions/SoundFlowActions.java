package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

/** Sound playback helpers for Flow actions. */
final class SoundFlowActions {

  private SoundFlowActions() {}

  static FlowEdgeAction playSound(
      ResourceLocation soundId,
      FlowActions.SoundAnchor anchor,
      Vec3 offset,
      float volume,
      float pitch,
      int delayTicks) {
    ResourceLocation resolvedSound = soundId;
    FlowActions.SoundAnchor resolvedAnchor =
        anchor == null ? FlowActions.SoundAnchor.PERFORMER : anchor;
    Vec3 safeOffset = offset == null ? Vec3.ZERO : offset;
    float sanitizedVolume = volume <= 0.0F ? 1.0F : volume;
    float sanitizedPitch = pitch <= 0.0F ? 1.0F : pitch;
    int delay = Math.max(0, delayTicks);
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (performer == null || resolvedSound == null) {
          return;
        }
        Level level = performer.level();
        if (!(level instanceof ServerLevel server)) {
          return;
        }

        Optional<Holder.Reference<SoundEvent>> holder =
            BuiltInRegistries.SOUND_EVENT.getHolder(resolvedSound);
        if (holder.isEmpty()) {
          ChestCavity.LOGGER.warn("[Flow] Unknown sound id {}", resolvedSound);
          return;
        }
        SoundEvent event = holder.get().value();

        Runnable task =
            () -> {
              LivingEntity anchorEntity = resolveAnchorEntity(performer, target, resolvedAnchor);
              if (anchorEntity == null) {
                return;
              }
              Vec3 base = anchorEntity.position();
              Vec3 finalPosition = base.add(safeOffset);
              SoundSource category = anchorEntity.getSoundSource();
              server.playSound(
                  null,
                  finalPosition.x,
                  finalPosition.y,
                  finalPosition.z,
                  event,
                  category,
                  sanitizedVolume,
                  sanitizedPitch);
            };

        if (delay > 0 && controller != null) {
          controller.schedule(gameTime + delay, task);
        } else {
          task.run();
        }
      }

      @Override
      public String describe() {
        return "play_sound(id="
            + resolvedSound
            + ", anchor="
            + (resolvedAnchor != null
                ? resolvedAnchor.name().toLowerCase(Locale.ROOT)
                : "performer")
            + ", delay="
            + delay
            + ")";
      }
    };
  }

  static FlowEdgeAction playSoundConditional(
      ResourceLocation soundId,
      FlowActions.SoundAnchor anchor,
      Vec3 offset,
      float volume,
      float pitch,
      int delayTicks,
      String variableName,
      double skipValue) {
    ResourceLocation resolvedSound = soundId;
    FlowActions.SoundAnchor resolvedAnchor =
        anchor == null ? FlowActions.SoundAnchor.PERFORMER : anchor;
    Vec3 safeOffset = offset == null ? Vec3.ZERO : offset;
    float sanitizedVolume = volume <= 0.0F ? 1.0F : volume;
    float sanitizedPitch = pitch <= 0.0F ? 1.0F : pitch;
    int delay = Math.max(0, delayTicks);
    double skip = Double.isNaN(skipValue) ? Double.NaN : skipValue;
    return new FlowEdgeAction() {
      @Override
      public void apply(
          Player performer, LivingEntity target, FlowController controller, long gameTime) {
        if (performer == null || resolvedSound == null) {
          return;
        }
        if (controller != null && variableName != null) {
          double value = controller.getDouble(variableName, Double.NaN);
          if (Double.isFinite(skip) && Double.isFinite(value) && Math.abs(value - skip) < 1.0E-4D) {
            return;
          }
        }
        Level level = performer.level();
        if (!(level instanceof ServerLevel server)) {
          return;
        }

        Optional<Holder.Reference<SoundEvent>> holder =
            BuiltInRegistries.SOUND_EVENT.getHolder(resolvedSound);
        if (holder.isEmpty()) {
          ChestCavity.LOGGER.warn("[Flow] Unknown sound id {}", resolvedSound);
          return;
        }
        SoundEvent event = holder.get().value();

        Runnable task =
            () -> {
              LivingEntity anchorEntity = resolveAnchorEntity(performer, target, resolvedAnchor);
              if (anchorEntity == null) {
                return;
              }
              Vec3 base = anchorEntity.position();
              Vec3 finalPosition = base.add(safeOffset);
              SoundSource category = anchorEntity.getSoundSource();
              server.playSound(
                  null,
                  finalPosition.x,
                  finalPosition.y,
                  finalPosition.z,
                  event,
                  category,
                  sanitizedVolume,
                  sanitizedPitch);
            };

        if (delay > 0 && controller != null) {
          controller.schedule(gameTime + delay, task);
        } else {
          task.run();
        }
      }

      @Override
      public String describe() {
        return "play_sound_conditional(id=" + resolvedSound + ")";
      }
    };
  }

  private static LivingEntity resolveAnchorEntity(
      Player performer, LivingEntity target, FlowActions.SoundAnchor anchor) {
    if (anchor == FlowActions.SoundAnchor.TARGET) {
      if (target != null && target.isAlive()) {
        return target;
      }
    }
    return performer;
  }
}
