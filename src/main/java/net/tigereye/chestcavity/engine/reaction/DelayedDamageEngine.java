package net.tigereye.chestcavity.engine.reaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** 延迟伤害调度（如“魂印回声”）。 */
public final class DelayedDamageEngine {
  private DelayedDamageEngine() {}

  private record DelayedDamage(int dueTick, UUID attacker, UUID target, float amount) {}

  private static final List<DelayedDamage> QUEUE = new ArrayList<>();

  public static void schedule(
      MinecraftServer server,
      LivingEntity attacker,
      LivingEntity target,
      double amount,
      int delayTicks) {
    if (server == null || target == null || delayTicks < 0 || amount <= 0.0D) return;
    int due = server.getTickCount() + delayTicks;
    UUID a = attacker != null ? attacker.getUUID() : null;
    UUID t = target.getUUID();
    QUEUE.add(new DelayedDamage(due, a, t, (float) amount));
  }

  public static void process(MinecraftServer server, long nowTick) {
    if (server == null || QUEUE.isEmpty()) return;
    Iterator<DelayedDamage> it = QUEUE.iterator();
    while (it.hasNext()) {
      DelayedDamage d = it.next();
      if (d.dueTick <= nowTick) {
        apply(server, d);
        it.remove();
      }
    }
  }

  private static void apply(MinecraftServer server, DelayedDamage d) {
    if (d == null) return;
    LivingEntity target = find(server, d.target);
    if (target == null || !target.isAlive()) return;
    LivingEntity attacker = d.attacker != null ? find(server, d.attacker) : null;
    if (attacker != null && target.isAlliedTo(attacker)) return;
    if (attacker != null) target.hurt(attacker.damageSources().mobAttack(attacker), d.amount);
    else target.hurt(target.damageSources().generic(), d.amount);
  }

  private static LivingEntity find(MinecraftServer server, UUID uuid) {
    if (uuid == null) return null;
    for (ServerLevel level : server.getAllLevels()) {
      var e = level.getEntity(uuid);
      if (e instanceof LivingEntity le) return le;
    }
    return null;
  }
}
