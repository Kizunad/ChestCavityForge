package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;

/**
 * Lightweight helpers to play custom sound events (data-driven via sounds.json).
 */
public final class SoundOps {

    private SoundOps() {}

    public static Optional<Holder.Reference<SoundEvent>> holder(ResourceLocation id) {
        if (id == null) return Optional.empty();
        return BuiltInRegistries.SOUND_EVENT.getHolder(id);
    }

    public static boolean play(ServerLevel level, double x, double y, double z,
                               ResourceLocation id, SoundSource source, float volume, float pitch) {
        if (level == null || id == null) return false;
        Optional<Holder.Reference<SoundEvent>> h = holder(id);
        if (h.isEmpty()) return false;
        level.playSound(null, x, y, z, h.get().value(), source, volume, pitch);
        return true;
    }

    public static boolean play(ServerLevel level, net.minecraft.world.phys.Vec3 pos,
                               ResourceLocation id, SoundSource source, float volume, float pitch) {
        if (pos == null) return false;
        return play(level, pos.x, pos.y, pos.z, id, source, volume, pitch);
    }
}

