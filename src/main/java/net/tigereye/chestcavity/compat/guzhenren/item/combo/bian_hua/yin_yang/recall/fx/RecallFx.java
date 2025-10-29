package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 归位 粒子/音效
 */
public final class RecallFx {
    private RecallFx() {}

    public static void play(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // 回环：脚下 CLOUD 圆环 + SWEEP_ATTACK
        for (int i = 0; i < 18; i++) {
            double angle = (i / 18.0) * Math.PI * 2;
            double offsetX = Math.cos(angle) * 1.2;
            double offsetZ = Math.sin(angle) * 1.2;
            level.sendParticles(ParticleTypes.CLOUD, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2;
            double offsetX = Math.cos(angle) * 1.0;
            double offsetZ = Math.sin(angle) * 1.0;
            level.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                x + offsetX,
                y + 0.3,
                z + offsetZ,
                1,
                0.0,
                0.0,
                0.0,
                0.0);
        }

        // 锚点闪烁：GLOW + END_ROD
        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 1.0;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 1.0;
            level.sendParticles(ParticleTypes.GLOW, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.02);
        }
        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + 0.2, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.05);
        }

        // 音效
        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}
