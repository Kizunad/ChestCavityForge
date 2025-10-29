package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 阴阳互渡 粒子/音效
 */
public final class TransferFx {
    private TransferFx() {}

    public static void play(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // 引导：胸口 END_ROD 涌出
        Vec3 lookDir = player.getLookAngle();
        for (int i = 0; i < 15; i++) {
            double dist = i * 0.5;
            double offsetX = lookDir.x * dist + (Math.random() - 0.5) * 0.3;
            double offsetY = 1.2 + lookDir.y * dist + (Math.random() - 0.5) * 0.3;
            double offsetZ = lookDir.z * dist + (Math.random() - 0.5) * 0.3;
            if (dist > 8.0) break;
            level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // 抵达：ENCHANT 漩涡 + SOUL
        Vec3 endpoint = pos.add(lookDir.scale(Math.min(8.0, 4.0)));
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2;
            double radius = 0.6;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            level.sendParticles(
                ParticleTypes.ENCHANT,
                endpoint.x + offsetX,
                endpoint.y + 1.0,
                endpoint.z + offsetZ,
                1,
                -offsetX * 0.1,
                0.0,
                -offsetZ * 0.1,
                0.05);
        }
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            level.sendParticles(
                ParticleTypes.SOUL,
                endpoint.x + offsetX,
                endpoint.y + 1.0 + offsetY,
                endpoint.z + offsetZ,
                1,
                0.0,
                0.05,
                0.0,
                0.02);
        }

        // 音效
        level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
