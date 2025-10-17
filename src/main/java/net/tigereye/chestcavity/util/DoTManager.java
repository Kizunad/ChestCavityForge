
package net.tigereye.chestcavity.util;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;

import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.util.reaction.ReactionRegistry;

/**
 * Central manager for simple time-based damage pulses (DoT).
 * Stores future pulses keyed by due server tick and executes them on tick.
 */
public final class DoTManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = true;

    private DoTManager() {}

    private static final NavigableMap<Integer, List<Pulse>> SCHEDULE = new TreeMap<>();

    /**
     * 可用于外部调试/查询的轻量视图。仅包含脉冲的关键信息，不暴露可变内部结构。
     */
    public record DoTEntry(
            int dueTick,
            UUID attackerUuid,
            UUID targetUuid,
            float amount,
            ResourceLocation typeId,
            @javax.annotation.Nullable ResourceLocation soundId,
            @javax.annotation.Nullable ResourceLocation fxId,
            FxAnchor fxAnchor
    ) {}

    /**
     * 针对某个目标（被 DoT 作用的实体）列出所有待执行脉冲的只读快照。
     * 仅在服务端有意义；调用时会复制当前队列，线程安全。
     */
    public static List<DoTEntry> getPendingForTarget(UUID targetUuid) {
        if (targetUuid == null) return List.of();
        List<DoTEntry> out = new ArrayList<>();
        synchronized (DoTManager.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                for (Pulse p : e.getValue()) {
                    if (targetUuid.equals(p.targetUuid)) {
                        out.add(toEntry(p));
                    }
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 针对某个攻击者（施加 DoT 的实体）列出所有待执行脉冲的只读快照。
     */
    public static List<DoTEntry> getPendingForAttacker(UUID attackerUuid) {
        if (attackerUuid == null) return List.of();
        List<DoTEntry> out = new ArrayList<>();
        synchronized (DoTManager.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                for (Pulse p : e.getValue()) {
                    if (attackerUuid.equals(p.attackerUuid)) {
                        out.add(toEntry(p));
                    }
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 汇总某个目标的 DoT：总脉冲数、预计总伤害、下一次触发 tick。
     */
    public static DoTSummary summarizeForTarget(UUID targetUuid) {
        if (targetUuid == null) return new DoTSummary(0, 0f, -1);
        int pulses = 0;
        float total = 0f;
        int next = -1;
        synchronized (DoTManager.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                int due = e.getKey();
                for (Pulse p : e.getValue()) {
                    if (targetUuid.equals(p.targetUuid)) {
                        pulses++;
                        total += p.amount;
                        if (next == -1 || due < next) next = due;
                    }
                }
            }
        }
        return new DoTSummary(pulses, total, next);
    }

    public record DoTSummary(int pulses, float totalExpectedDamage, int nextDueTick) {}

    private static DoTEntry toEntry(Pulse p) {
        ResourceLocation soundId = null;
        if (p.sound != null) {
            // SoundEvent#getLocation 在 1.21 可用；不同版本可视需要调整。
            soundId = p.sound.getLocation();
        }
        return new DoTEntry(p.dueTick, p.attackerUuid, p.targetUuid, p.amount, p.typeId, soundId, p.fxId, p.fxAnchor);
    }

    public static void schedulePerSecond(LivingEntity attacker,
                                         LivingEntity target,
                                         double perSecondDamage,
                                         int durationSeconds,
                                         @javax.annotation.Nullable SoundEvent tickSound,
                                         float volume,
                                         float pitch,
                                         ResourceLocation typeId,
                                         @javax.annotation.Nullable ResourceLocation fxId,
                                         FxAnchor fxAnchor,
                                         Vec3 fxOffset,
                                         float fxIntensity) {
        if (typeId == null) {
            throw new IllegalArgumentException("DoT typeId must not be null. Register a proper DoTTypes id.");
        }
        if (attacker == null || target == null || durationSeconds <= 0 || perSecondDamage <= 0.0) {
            return;
        }
        ServerLevel serverLevel = target.level() instanceof ServerLevel s ? s : null;
        if (serverLevel == null) return;
        MinecraftServer server = serverLevel.getServer();
        int now = server.getTickCount();
        Vec3 safeOffset = fxOffset == null ? Vec3.ZERO : fxOffset;
        FxAnchor anchor = fxAnchor == null ? FxAnchor.TARGET : fxAnchor;
        float intensity = fxIntensity <= 0.0f ? 1.0f : fxIntensity;
        for (int i = 1; i <= durationSeconds; i++) {
            int due = now + i * 20;
            enqueue(new Pulse(due,
                    attacker.getUUID(),
                    target.getUUID(),
                    (float) perSecondDamage,
                    typeId,
                    tickSound,
                    volume,
                    pitch,
                    fxId,
                    anchor,
                    safeOffset,
                    intensity));
        }
        if (DEBUG) {
            LOGGER.info("[dot] queued DoT type={} seconds={} attacker={} target={} dps={}",
                    typeId,
                    durationSeconds,
                    attacker.getName().getString(),
                    target.getName().getString(),
                    perSecondDamage);
        }
    }

    private static synchronized void enqueue(Pulse pulse) {
        SCHEDULE.computeIfAbsent(pulse.dueTick, k -> new ArrayList<>()).add(pulse);
    }

    public static int cancelAttacker(LivingEntity attacker) {
        return attacker == null ? 0 : cancelAttacker(attacker.getUUID());
    }

    public static int cancelAttacker(UUID attackerUuid) {
        if (attackerUuid == null) {
            return 0;
        }
        int removed = 0;
        synchronized (DoTManager.class) {
            Iterator<Map.Entry<Integer, List<Pulse>>> iterator = SCHEDULE.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<Pulse>> entry = iterator.next();
                List<Pulse> pulses = entry.getValue();
                int before = pulses.size();
                pulses.removeIf(pulse -> attackerUuid.equals(pulse.attackerUuid));
                removed += before - pulses.size();
                if (pulses.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        if (removed > 0 && DEBUG) {
            LOGGER.info("[dot] cleared {} scheduled pulses for attacker {}", removed, attackerUuid);
        }
        return removed;
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
        // 先走反应系统：若触发了反应且要求取消本次伤害，则直接返回
        if (!ReactionRegistry.preApplyDoT(server, pulse.typeId, attacker, target)) {
            if (DEBUG) {
                LOGGER.info("[dot] cancelled by reaction: type={} target={}", pulse.typeId, target.getName().getString());
            }
            return;
        }
        target.hurt(source, pulse.amount);
        if (pulse.sound != null) {
            ServerLevel level = (ServerLevel) target.level();
            level.playSound(null, target.blockPosition(), pulse.sound, SoundSource.PLAYERS, pulse.volume, pulse.pitch);
        }
        if (pulse.fxId != null && target.level() instanceof ServerLevel level) {
            LivingEntity anchor = pulse.fxAnchor == FxAnchor.ATTACKER ? attacker : target;
            if (anchor != null) {
                Vec3 origin = new Vec3(anchor.getX(), anchor.getY() + anchor.getBbHeight() * 0.5D, anchor.getZ()).add(pulse.fxOffset);
                Vec3 direction = anchor.getLookAngle();
                ServerPlayer performer = attacker instanceof ServerPlayer sp ? sp : null;
                AbilityFxDispatcher.play(level, pulse.fxId, origin, direction, direction, performer, anchor, pulse.fxIntensity);
            }
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
        final ResourceLocation typeId;
        final SoundEvent sound;
        final float volume;
        final float pitch;
        final ResourceLocation fxId;
        final FxAnchor fxAnchor;
        final Vec3 fxOffset;
        final float fxIntensity;
        Pulse(int dueTick, UUID attackerUuid, UUID targetUuid, float amount,
              ResourceLocation typeId,
              SoundEvent sound, float volume, float pitch,
              ResourceLocation fxId, FxAnchor fxAnchor, Vec3 fxOffset, float fxIntensity) {
            this.dueTick = dueTick;
            this.attackerUuid = attackerUuid;
            this.targetUuid = targetUuid;
            this.amount = amount;
            this.typeId = (typeId == null ? DoTTypes.GENERIC : typeId);
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.fxId = fxId;
            this.fxAnchor = fxAnchor;
            this.fxOffset = fxOffset;
            this.fxIntensity = fxIntensity;
        }
    }

    public enum FxAnchor {
        ATTACKER,
        TARGET
    }
}
