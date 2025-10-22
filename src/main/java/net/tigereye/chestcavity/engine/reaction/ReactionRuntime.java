package net.tigereye.chestcavity.engine.reaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** 反应运行时状态：屏蔽窗口/速率限制等。 目前承载火衣-油涂层反应的屏蔽窗口管理。 */
public final class ReactionRuntime {
  private ReactionRuntime() {}

  private static final Map<UUID, Long> FIRE_AURA_BLOCK_UNTIL = new HashMap<>();

  public static void purge(long nowTick) {
    FIRE_AURA_BLOCK_UNTIL.entrySet().removeIf(e -> e.getValue() <= nowTick);
  }

  public static void blockFireOil(LivingEntity attacker, int durationTicks) {
    if (attacker == null || durationTicks <= 0) return;
    if (!(attacker.level() instanceof ServerLevel server)) return;
    long now = server.getServer().getTickCount();
    FIRE_AURA_BLOCK_UNTIL.put(attacker.getUUID(), now + durationTicks);
  }

  public static boolean isFireOilBlocked(LivingEntity attacker) {
    if (attacker == null) return false;
    if (!(attacker.level() instanceof ServerLevel server)) return false;
    long now = server.getServer().getTickCount();
    return FIRE_AURA_BLOCK_UNTIL.getOrDefault(attacker.getUUID(), 0L) > now;
  }

  public static boolean isFireOilBlocked(LivingEntity attacker, long nowTick) {
    if (attacker == null) return false;
    return FIRE_AURA_BLOCK_UNTIL.getOrDefault(attacker.getUUID(), 0L) > nowTick;
  }
}
