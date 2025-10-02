
package net.tigereye.chestcavity.util;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;

/**
 * Central manager for simple time-based damage pulses (DoT).
 * Stores future pulses keyed by due server tick and executes them on tick.
 */
public final class DoTManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = true;

    private DoTManager() {}

    private static final NavigableMap<Integer, List<Pulse>> SCHEDULE = new TreeMap<>();

    public static void schedulePerSecond(LivingEntity attacker,
                                         LivingEntity target,
                                         double perSecondDamage,
                                         int durationSeconds,
                                         @javax.annotation.Nullable SoundEvent tickSound,
                                         float volume,
                                         float pitch) {
        if (attacker == null || target == null || durationSeconds <= 0 || perSecondDamage <= 0.0) {
            return;
        }
        ServerLevel serverLevel = attacker.level() instanceof ServerLevel s ? s : null;
        if (serverLevel == null) return;
        MinecraftServer server = serverLevel.getServer();
        int now = server.getTickCount();
        for (int i = 1; i <= durationSeconds; i++) {
            int due = now + i * 20;
            enqueue(new Pulse(due,
                    attacker.getUUID(),
                    target.getUUID(),
                    (float) perSecondDamage,
                    tickSound,
                    volume,
                    pitch));
        }
        if (DEBUG) {
            LOGGER.info("[dot] queued DoT pulses seconds={} attacker={} target={} amountPerSecond={}",
                    durationSeconds,
                    attacker.getName().getString(),
                    target.getName().getString(),
                    perSecondDamage);
        }
    }

    private static synchronized void enqueue(Pulse pulse) {
        SCHEDULE.computeIfAbsent(pulse.dueTick, k -> new ArrayList<>()).add(pulse);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        List<Map.Entry<Integer, List<Pulse>>> due = new ArrayList<>();
        synchronized (DoTManager.class) {
            while (!SCHEDULE.isEmpty()) {
                Map.Entry<Integer, List<Pulse>> first = SCHEDULE.firstEntry();
                if (first.getKey() > now) break;
                due.add(SCHEDULE.pollFirstEntry());
            }
        }
        if (due.isEmpty()) return;
        for (Map.Entry<Integer, List<Pulse>> entry : due) {
            for (Pulse pulse : entry.getValue()) {
                executePulse(event.getServer(), pulse);
            }
        }
    }

    private static void executePulse(MinecraftServer server, Pulse pulse) {
        LivingEntity target = findLiving(server, pulse.targetUuid);
        if (target == null || !target.isAlive()) return;
        LivingEntity attacker = findLiving(server, pulse.attackerUuid);
        if (attacker != null && target.isAlliedTo(attacker)) return;
        DamageSource source;
        if (attacker instanceof Player player) {
            source = player.damageSources().playerAttack(player);
        } else if (attacker != null) {
            source = attacker.damageSources().mobAttack(attacker);
        } else {
            source = target.damageSources().generic();
        }
        target.hurt(source, pulse.amount);
        if (pulse.sound != null) {
            ServerLevel level = (ServerLevel) target.level();
            level.playSound(null, target.blockPosition(), pulse.sound, SoundSource.PLAYERS, pulse.volume, pulse.pitch);
        }
        if (DEBUG) {
            LOGGER.info("[dot] apply DoT dueTick={} attacker={} target={} damage={}",
                    pulse.dueTick,
                    attacker != null ? attacker.getName().getString() : "<none>",
                    target.getName().getString(),
                    pulse.amount);
        }
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof LivingEntity le) {
                return le;
            }
        }
        return null;
    }

    private static final class Pulse {
        final int dueTick;
        final UUID attackerUuid;
        final UUID targetUuid;
        final float amount;
        final SoundEvent sound;
        final float volume;
        final float pitch;
        Pulse(int dueTick, UUID attackerUuid, UUID targetUuid, float amount,
              SoundEvent sound, float volume, float pitch) {
            this.dueTick = dueTick;
            this.attackerUuid = attackerUuid;
            this.targetUuid = targetUuid;
            this.amount = amount;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
