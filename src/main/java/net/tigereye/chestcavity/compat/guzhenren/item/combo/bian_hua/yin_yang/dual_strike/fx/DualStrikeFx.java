package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 两界同击 粒子/音效
 */
public final class DualStrikeFx {
    private DualStrikeFx() {}

    public static void playActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = player.position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        // 胸前漂浮 ENCHANT 微粒
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = 1.2 + (Math.random() - 0.5) * 0.3;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.ENCHANT, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.02, 0.0, 0.01);
        }

        // 音效
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.7f);
    }

    public static void playHit(ServerPlayer player, LivingEntity target, boolean bothHit) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.7, 0);
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        // 弹道拖尾：CRIT
        Vec3 direction = targetPos.subtract(playerPos);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.3);
        int steps = Math.min(12, (int) (distance / 0.3));
        for (int i = 0; i < steps; i++) {
            Vec3 point = playerPos.add(step.scale(i));
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // 命中点：CRIT + ELECTRIC_SPARK
        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            level.sendParticles(
                ParticleTypes.CRIT,
                targetPos.x + offsetX,
                targetPos.y + offsetY,
                targetPos.z + offsetZ,
                1,
                0.0,
                0.0,
                0.0,
                0.05);
        }
        for (int i = 0; i < 8; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                targetPos.x + offsetX,
                targetPos.y + offsetY,
                targetPos.z + offsetZ,
                1,
                0.0,
                0.0,
                0.0,
                0.05);
        }

        // 投影触发：ENCHANT 向外炸散
        if (bothHit) {
            for (int i = 0; i < 18; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 1.0;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double offsetY = (Math.random() - 0.5) * 0.8;
                level.sendParticles(
                    ParticleTypes.ENCHANT,
                    targetPos.x + offsetX,
                    targetPos.y + offsetY,
                    targetPos.z + offsetZ,
                    1,
                    offsetX * 0.2,
                    offsetY * 0.2,
                    offsetZ * 0.2,
                    0.1);
            }
            // 音效
            level.playSound(
                null,
                targetPos.x,
                targetPos.y,
                targetPos.z,
                SoundEvents.TRIDENT_THUNDER,
                SoundSource.PLAYERS,
                0.6f,
                1.25f);
        }
    }
}
