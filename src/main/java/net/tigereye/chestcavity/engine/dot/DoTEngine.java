package net.tigereye.chestcavity.engine.dot;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.engine.TickEngineHub;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.reaction.api.ReactionAPI;
import org.slf4j.Logger;

import java.util.*;

/**
 * DoT 核心引擎。
 * - 负责持续伤害的调度、聚合与执行；
 * - 在 ServerTick(Post) 由 TickEngineHub 以优先级晚于 Reaction 执行。
 */
public final class DoTEngine {

    private DoTEngine() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean debugEnabled() { return Boolean.getBoolean("chestcavity.debugDoT"); }

    // ---- 类型 ----
    public enum FxAnchor { ATTACKER, TARGET }

    public record DoTEntry(
            int dueTick,
            UUID attackerUuid,
            UUID targetUuid,
            float amount,
            ResourceLocation typeId,
            ResourceLocation soundId,
            ResourceLocation fxId,
            FxAnchor fxAnchor
    ) {}

    public record DoTSummary(int pulses, float totalExpectedDamage, int nextDueTick) {}

    // ---- 存储队列 ----
    private static final NavigableMap<Integer, List<Pulse>> SCHEDULE = new TreeMap<>();

    // ---- 引导 ----
    public static void bootstrap() {
        TickEngineHub.register(TickEngineHub.PRIORITY_DOT, DoTEngine::handleServerTick);
    }

    // ---- API ----
    public static void schedulePerSecond(LivingEntity attacker,
                                         LivingEntity target,
                                         double perSecondDamage,
                                         int durationSeconds,
                                         SoundEvent tickSound,
                                         float volume,
                                         float pitch,
                                         ResourceLocation typeId,
                                         ResourceLocation fxId,
                                         FxAnchor fxAnchor,
                                         Vec3 fxOffset,
                                         float fxIntensity) {
        if (typeId == null) throw new IllegalArgumentException("DoT typeId must not be null. Use DoTTypes.");
        if (attacker == null || target == null || durationSeconds <= 0 || perSecondDamage <= 0.0) return;
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
        if (debugEnabled()) {
            LOGGER.info("[dot] queued DoT type={} seconds={} attacker={} target={} dps={}",
                    typeId, durationSeconds,
                    attacker.getName().getString(), target.getName().getString(), perSecondDamage);
        }
    }

    private static synchronized void enqueue(Pulse pulse) {
        SCHEDULE.computeIfAbsent(pulse.dueTick, k -> new ArrayList<>()).add(pulse);
    }

    public static int cancelAttacker(LivingEntity attacker) { return attacker == null ? 0 : cancelAttacker(attacker.getUUID()); }

    public static int cancelAttacker(UUID attackerUuid) {
        if (attackerUuid == null) return 0;
        int removed = 0;
        synchronized (DoTEngine.class) {
            Iterator<Map.Entry<Integer, List<Pulse>>> iterator = SCHEDULE.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<Pulse>> entry = iterator.next();
                List<Pulse> pulses = entry.getValue();
                int before = pulses.size();
                pulses.removeIf(pulse -> attackerUuid.equals(pulse.attackerUuid));
                removed += before - pulses.size();
                if (pulses.isEmpty()) iterator.remove();
            }
        }
        if (removed > 0 && debugEnabled()) {
            LOGGER.info("[dot] cleared {} scheduled pulses for attacker {}", removed, attackerUuid);
        }
        return removed;
    }

    public static int cancel(LivingEntity attacker, LivingEntity target, ResourceLocation typeId) {
        return cancel(attacker != null ? attacker.getUUID() : null,
                target != null ? target.getUUID() : null,
                typeId);
    }

    public static int cancel(UUID attackerUuid, UUID targetUuid, ResourceLocation typeId) {
        if (typeId == null) {
            return 0;
        }
        int removed = 0;
        synchronized (DoTEngine.class) {
            Iterator<Map.Entry<Integer, List<Pulse>>> iterator = SCHEDULE.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<Pulse>> entry = iterator.next();
                List<Pulse> pulses = entry.getValue();
                int before = pulses.size();
                pulses.removeIf(pulse -> typeId.equals(pulse.typeId)
                        && (attackerUuid == null || attackerUuid.equals(pulse.attackerUuid))
                        && (targetUuid == null || targetUuid.equals(pulse.targetUuid)));
                removed += before - pulses.size();
                if (pulses.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        if (removed > 0 && debugEnabled()) {
            LOGGER.info("[dot] cleared {} scheduled pulses for type={} attacker={} target={}",
                    removed, typeId, attackerUuid, targetUuid);
        }
        return removed;
    }

    public static List<DoTEntry> getPendingForTarget(UUID targetUuid) {
        if (targetUuid == null) return List.of();
        List<DoTEntry> out = new ArrayList<>();
        synchronized (DoTEngine.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                for (Pulse p : e.getValue()) {
                    if (targetUuid.equals(p.targetUuid)) out.add(toEntry(p));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static List<DoTEntry> getPendingForAttacker(UUID attackerUuid) {
        if (attackerUuid == null) return List.of();
        List<DoTEntry> out = new ArrayList<>();
        synchronized (DoTEngine.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                for (Pulse p : e.getValue()) {
                    if (attackerUuid.equals(p.attackerUuid)) out.add(toEntry(p));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static DoTSummary summarizeForTarget(UUID targetUuid) {
        if (targetUuid == null) return new DoTSummary(0, 0f, -1);
        int pulses = 0; float total = 0f; int next = -1;
        synchronized (DoTEngine.class) {
            for (Map.Entry<Integer, List<Pulse>> e : SCHEDULE.entrySet()) {
                int due = e.getKey();
                for (Pulse p : e.getValue()) {
                    if (targetUuid.equals(p.targetUuid)) {
                        pulses++; total += p.amount; if (next == -1 || due < next) next = due;
                    }
                }
            }
        }
        return new DoTSummary(pulses, total, next);
    }

    private static DoTEntry toEntry(Pulse p) {
        ResourceLocation soundId = p.sound != null ? p.sound.getLocation() : null;
        return new DoTEntry(p.dueTick, p.attackerUuid, p.targetUuid, p.amount, p.typeId, soundId, p.fxId, p.fxAnchor);
    }

    // ---- Tick 执行：聚合与结算 ----
    public static void handleServerTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        List<Map.Entry<Integer, List<Pulse>>> due = new ArrayList<>();
        synchronized (DoTEngine.class) {
            while (!SCHEDULE.isEmpty()) {
                Map.Entry<Integer, List<Pulse>> first = SCHEDULE.firstEntry();
                if (first.getKey() > now) break;
                due.add(SCHEDULE.pollFirstEntry());
            }
        }
        if (due.isEmpty()) return;
        Map<UUID, Map<UUID, Float>> grouped = new HashMap<>(); // target -> (attacker|null -> totalDamage)
        Map<UUID, LivingEntity> targetCache = new HashMap<>();
        Map<UUID, LivingEntity> attackerCache = new HashMap<>();
        UUID NONE = new UUID(0L, 0L);
        for (Map.Entry<Integer, List<Pulse>> entry : due) {
            for (Pulse pulse : entry.getValue()) {
                LivingEntity target = targetCache.computeIfAbsent(pulse.targetUuid, id -> findLiving(event.getServer(), id));
                if (target == null || !target.isAlive()) continue;
                LivingEntity attacker = pulse.attackerUuid != null ?
                        attackerCache.computeIfAbsent(pulse.attackerUuid, id -> findLiving(event.getServer(), id)) : null;
                if (attacker != null && target.isAlliedTo(attacker)) continue;
                // 反应判定：若取消则跳过本次伤害
                if (!ReactionAPI.get().preApplyDoT(event.getServer(), pulse.typeId, attacker, target)) continue;
                UUID atkKey = attacker != null ? attacker.getUUID() : NONE;
                grouped.computeIfAbsent(target.getUUID(), k -> new HashMap<>()).merge(atkKey, pulse.amount, Float::sum);
            }
        }
        for (Map.Entry<UUID, Map<UUID, Float>> e : grouped.entrySet()) {
            LivingEntity target = targetCache.get(e.getKey());
            if (target == null || !target.isAlive()) continue;
            for (Map.Entry<UUID, Float> g : e.getValue().entrySet()) {
                float total = g.getValue(); if (total <= 0f) continue;
                LivingEntity attacker = !g.getKey().equals(NONE) ? attackerCache.get(g.getKey()) : null;
                DamageSource source;
                if (attacker instanceof Player player) source = player.damageSources().playerAttack(player);
                else if (attacker != null) source = attacker.damageSources().mobAttack(attacker);
                else source = target.damageSources().generic();
                target.hurt(source, total);
            }
        }
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof LivingEntity le) return le;
        }
        return null;
    }

    private static final class Pulse {
        final int dueTick; final UUID attackerUuid; final UUID targetUuid; final float amount; final ResourceLocation typeId;
        final SoundEvent sound; final float volume; final float pitch; final ResourceLocation fxId; final FxAnchor fxAnchor; final Vec3 fxOffset; final float fxIntensity;
        Pulse(int dueTick, UUID attackerUuid, UUID targetUuid, float amount,
              ResourceLocation typeId, SoundEvent sound, float volume, float pitch,
              ResourceLocation fxId, FxAnchor fxAnchor, Vec3 fxOffset, float fxIntensity) {
            this.dueTick = dueTick; this.attackerUuid = attackerUuid; this.targetUuid = targetUuid; this.amount = amount;
            this.typeId = (typeId == null ? DoTTypes.GENERIC : typeId); this.sound = sound; this.volume = volume; this.pitch = pitch;
            this.fxId = fxId; this.fxAnchor = fxAnchor; this.fxOffset = fxOffset; this.fxIntensity = fxIntensity;
        }
    }
}
