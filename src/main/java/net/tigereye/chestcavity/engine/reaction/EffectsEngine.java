package net.tigereye.chestcavity.engine.reaction;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import org.slf4j.Logger;

import java.util.*;

/**
 * 反应效果执行引擎：将 AoE/Explosion/Residue 等重型动作汇总到统一队列，
 * 在 ServerTick(Post) 执行，支持限流与降级。
 */
public final class EffectsEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private EffectsEngine() {}

    private static final List<Job> QUEUE = new ArrayList<>();

    // ---- enqueue APIs ----
    public static void queueAoEDamage(ServerLevel level, double x, double y, double z,
                                      float radius, double damage,
                                      LivingEntity attacker) {
        if (level == null || radius <= 0.0F || damage <= 0.0D) return;
        QUEUE.add(Job.aoe(level, x, y, z, radius, (float) damage, attacker));
    }
    public static void queueAoEDamage(ServerLevel level, double x, double y, double z,
                                      float radius, double damage,
                                      LivingEntity attacker,
                                      VisualTheme theme) {
        if (level == null || radius <= 0.0F || damage <= 0.0D) return;
        QUEUE.add(Job.aoe(level, x, y, z, radius, (float) damage, attacker, theme));
    }
    public static void queueExplosion(ServerLevel level, double x, double y, double z, float power,
                                      LivingEntity attacker) {
        queueExplosion(level, x, y, z, power, attacker, false);
    }
    public static void queueExplosion(ServerLevel level, double x, double y, double z, float power,
                                      LivingEntity attacker, boolean forceExplosion) {
        if (level == null || power <= 0.0F) return;
        QUEUE.add(Job.explosion(level, x, y, z, power, attacker, forceExplosion, VisualTheme.GENERIC));
    }
    public static void queueExplosion(ServerLevel level, double x, double y, double z, float power,
                                      LivingEntity attacker, boolean forceExplosion,
                                      VisualTheme theme) {
        if (level == null || power <= 0.0F) return;
        QUEUE.add(Job.explosion(level, x, y, z, power, attacker, forceExplosion, theme));
    }

    // ---- process on tick ----
    public static void process(MinecraftServer server) {
        if (QUEUE.isEmpty() || server == null) return;
        CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
        int maxJobs = Math.max(1, C.globalMaxJobsPerTick);
        int executed = 0;
        List<Job> snapshot = new ArrayList<>(QUEUE);
        QUEUE.clear();
        float degradeFactor = snapshot.size() > maxJobs ? 0.7F : 1.0F;
        int perAttackerMax = Math.max(1, C.perAttackerMaxJobsPerTick);
        Map<UUID, Integer> attackerBudget = new HashMap<>();
        for (int i = 0; i < snapshot.size() && executed < maxJobs; i++) {
            Job job = snapshot.get(i);
            if (job.kind == JobKind.AOE || job.kind == JobKind.EXPLOSION) {
                UUID a = job.attackerUuid;
                if (a != null) {
                    int cnt = attackerBudget.getOrDefault(a, 0);
                    if (cnt >= perAttackerMax) continue;
                    attackerBudget.put(a, cnt + 1);
                }
            }
            switch (job.kind) {
                case AOE -> runAoE(job, C, degradeFactor);
                case EXPLOSION -> runExplosion(job, C, degradeFactor);
            }
            executed++;
        }
    }

    private static void runAoE(Job job, CCConfig.ReactionConfig C, float degrade) {
        if (job.level == null) return;
        float r = Math.max(0.5F, job.radius * degrade);
        float dmg = (float) Math.max(0.0D, job.damage * degrade);
        int maxEntities = Math.max(1, C.maxAoEEntitiesPerJob);
        var aabb = new net.minecraft.world.phys.AABB(job.x - r, job.y - r, job.z - r, job.x + r, job.y + r, job.z + r);
        List<Entity> ents = job.level.getEntities(null, aabb);
        int applied = 0;
        for (Entity e : ents) {
            if (!(e instanceof LivingEntity le)) continue;
            if (job.attackerUuid != null) {
                LivingEntity attacker = find(job.level.getServer(), job.attackerUuid);
                if (attacker != null && le.isAlliedTo(attacker)) continue;
                if (attacker != null) le.hurt(attacker.damageSources().mobAttack(attacker), dmg);
                else le.hurt(le.damageSources().generic(), dmg);
            } else {
                le.hurt(le.damageSources().generic(), dmg);
            }
            applied++;
            if (applied >= maxEntities) break;
        }
        if (C.vfxEnable) spawnVfx(job, r, job.theme);
        if (C.sfxEnable) playSfx(job, job.theme, C);
    }

    private static void runExplosion(Job job, CCConfig.ReactionConfig C, float degrade) {
        if (job.level == null) return;
        float power = Math.max(0.1F, job.power * degrade);
        try {
            if (C.useAoEForAll && !job.forceExplosion) {
                float r = Math.max(0.5F, power * 1.6F);
                double dmg = power * 2.0F;
                runAoE(new Job(JobKind.AOE, job.level, job.x, job.y, job.z, r, (float) dmg, 0, 0, 0.0F, job.attackerUuid, false, job.theme), C, degrade);
            } else {
                job.level.explode(find(job.level.getServer(), job.attackerUuid), job.x, job.y, job.z, power, Level.ExplosionInteraction.NONE);
                if (ChestCavity.config.REACTION.vfxEnable) spawnVfx(job, power * 1.2F, VisualTheme.FIRE);
                if (ChestCavity.config.REACTION.sfxEnable) playSfx(job, VisualTheme.FIRE, ChestCavity.config.REACTION);
            }
        } catch (Throwable ignored) {}
    }

    private static LivingEntity find(MinecraftServer server, UUID id) {
        if (server == null || id == null) return null;
        for (ServerLevel l : server.getAllLevels()) {
            var e = l.getEntity(id);
            if (e instanceof LivingEntity le) return le;
        }
        return null;
    }

    public enum VisualTheme { GENERIC, FIRE, STEAM, FROST, SOUL, CORROSION, BLOOD }
    private enum JobKind { AOE, EXPLOSION }

    private static final class Job {
        final JobKind kind; final ServerLevel level; final double x,y,z;
        final float radius; final float damage; final int durationTicks; final int slowAmplifier;
        final float power; final UUID attackerUuid; final boolean forceExplosion; final VisualTheme theme;
        Job(JobKind kind, ServerLevel level, double x, double y, double z,
            float radius, float damage, int durationTicks, int slowAmplifier, float power,
            UUID attackerUuid, boolean forceExplosion, VisualTheme theme) {
            this.kind = kind; this.level = level; this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.damage = damage; this.durationTicks = durationTicks; this.slowAmplifier = slowAmplifier;
            this.power = power; this.attackerUuid = attackerUuid; this.forceExplosion = forceExplosion; this.theme = theme == null ? VisualTheme.GENERIC : theme;
        }
        static Job aoe(ServerLevel lvl, double x, double y, double z, float r, float dmg, LivingEntity attacker) {
            return new Job(JobKind.AOE, lvl, x, y, z, r, dmg, 0, 0, 0.0F, attacker == null ? null : attacker.getUUID(), false, VisualTheme.GENERIC);
        }
        static Job aoe(ServerLevel lvl, double x, double y, double z, float r, float dmg, LivingEntity attacker, VisualTheme theme) {
            return new Job(JobKind.AOE, lvl, x, y, z, r, dmg, 0, 0, 0.0F, attacker == null ? null : attacker.getUUID(), false, theme);
        }
        static Job explosion(ServerLevel lvl, double x, double y, double z, float power, LivingEntity attacker, boolean forceExplosion, VisualTheme theme) {
            return new Job(JobKind.EXPLOSION, lvl, x, y, z, 0.0F, 0.0F, 0, 0, power, attacker == null ? null : attacker.getUUID(), forceExplosion, theme);
        }
    }

    private static void spawnVfx(Job job, float scale, VisualTheme theme) {
        if (job.level == null) return;
        float s = Math.max(0.5F, scale) * ChestCavity.config.REACTION.vfxIntensity;
        int count = (int) Math.ceil(6 * s);
        for (int i = 0; i < count; i++) {
            double ox = (job.level.getRandom().nextDouble() - 0.5D) * s;
            double oy = job.level.getRandom().nextDouble() * (0.2D + 0.1D * s);
            double oz = (job.level.getRandom().nextDouble() - 0.5D) * s;
            switch (theme) {
                case FIRE -> {
                    job.level.sendParticles(ParticleTypes.FLAME, job.x, job.y, job.z, 1, ox, oy, oz, 0.01D);
                    job.level.sendParticles(ParticleTypes.SMOKE, job.x, job.y, job.z, 1, ox * 0.8D, oy * 0.5D, oz * 0.8D, 0.01D);
                }
                case STEAM -> {
                    job.level.sendParticles(ParticleTypes.CLOUD, job.x, job.y, job.z, 1, ox, oy, oz, 0.02D);
                    job.level.sendParticles(ParticleTypes.SMOKE, job.x, job.y, job.z, 1, ox, oy * 0.6D, oz, 0.005D);
                }
                case FROST -> job.level.sendParticles(ParticleTypes.POOF, job.x, job.y, job.z, 1, ox, oy, oz, 0.01D);
                case SOUL -> job.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, job.x, job.y, job.z, 1, ox, oy, oz, 0.01D);
                case BLOOD -> job.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, job.x, job.y + 0.2D, job.z, 1, ox, oy * 0.8D, oz, 0.02D);
                case CORROSION, GENERIC -> job.level.sendParticles(ParticleTypes.SMOKE, job.x, job.y, job.z, 1, ox, oy * 0.4D, oz, 0.005D);
            }
        }
    }
    private static void playSfx(Job job, VisualTheme theme, CCConfig.ReactionConfig C) {
        if (job.level == null) return;
        float base = Math.max(0.2F, C.sfxVolume);
        float pitch = 1.0F + (job.level.getRandom().nextFloat() - 0.5F) * C.sfxPitchVariance * 2.0F;
        switch (theme) {
            case FIRE -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, base, pitch);
            case STEAM -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, base, pitch);
            case FROST -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, base * 0.8F, pitch + 0.2F);
            case SOUL -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, base * 0.7F, pitch);
            case BLOOD -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, base * 0.6F, pitch * 0.9F);
            case CORROSION, GENERIC -> job.level.playSound(null, job.x, job.y, job.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, base * 0.7F, pitch);
        }
    }
}

