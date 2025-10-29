package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 太极错位 粒子/音效
 */
public final class TaiJiSwapFx {
    private TaiJiSwapFx() {}

    public static void play(ServerPlayer player, Vec3 origin, Vec3 destination, boolean withinWindow) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 起点门：PORTAL + REVERSE_PORTAL
        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * 1.2;
            double offsetY = Math.random() * 2.0;
            double offsetZ = (Math.random() - 0.5) * 1.2;
            level.sendParticles(
                ParticleTypes.PORTAL,
                origin.x + offsetX,
                origin.y + offsetY,
                origin.z + offsetZ,
                1,
                0.0,
                0.1,
                0.0,
                0.05);
        }
        for (int i = 0; i < 16; i++) {
            double offsetX = (Math.random() - 0.5) * 1.2;
            double offsetY = Math.random() * 2.0;
            double offsetZ = (Math.random() - 0.5) * 1.2;
            level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                origin.x + offsetX,
                origin.y + offsetY,
                origin.z + offsetZ,
                1,
                0.0,
                0.1,
                0.0,
                0.05);
        }

        // 终点门：PORTAL + REVERSE_PORTAL
        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * 1.2;
            double offsetY = Math.random() * 2.0;
            double offsetZ = (Math.random() - 0.5) * 1.2;
            level.sendParticles(
                ParticleTypes.PORTAL,
                destination.x + offsetX,
                destination.y + offsetY,
                destination.z + offsetZ,
                1,
                0.0,
                0.1,
                0.0,
                0.05);
        }
        for (int i = 0; i < 16; i++) {
            double offsetX = (Math.random() - 0.5) * 1.2;
            double offsetY = Math.random() * 2.0;
            double offsetZ = (Math.random() - 0.5) * 1.2;
            level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                destination.x + offsetX,
                destination.y + offsetY,
                destination.z + offsetZ,
                1,
                0.0,
                0.1,
                0.0,
                0.05);
        }

        // 切割轨迹：两点连线 END_ROD
        Vec3 direction = destination.subtract(origin);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.6);
        int steps = Math.min(16, (int) (distance / 0.6));
        for (int i = 0; i < steps; i++) {
            Vec3 point = origin.add(step.scale(i));
            level.sendParticles(
                ParticleTypes.END_ROD,
                point.x,
                point.y + 1.0,
                point.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0);
        }

        // 无敌提示：头顶闪烁火花
        if (!withinWindow) {
            for (int i = 0; i < 6; i++) {
                double offsetX = (Math.random() - 0.5) * 0.3;
                double offsetZ = (Math.random() - 0.5) * 0.3;
                level.sendParticles(
                    ParticleTypes.SMALL_FLAME,
                    destination.x + offsetX,
                    destination.y + 2.2,
                    destination.z + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.02);
            }
        }

        // 音效
        level.playSound(
            null,
            destination.x,
            destination.y,
            destination.z,
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0f,
            1.0f);
        if (withinWindow) {
            level.playSound(
                null,
                destination.x,
                destination.y,
                destination.z,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.6f,
                1.5f);
        }
    }
}
