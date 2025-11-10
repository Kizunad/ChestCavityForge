package net.tigereye.chestcavity.compat.common.agent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;

/** 轻量运行期临时状态器（仅当前进程内存，不做持久化）： - 管理“按 ID 安装的 transient 属性修饰器”的 TTL 与回滚 - NPC/玩家通用 */
public final class AgentTempState {

  private AgentTempState() {}

  private static final Map<UUID, Map<ResourceLocation, Applied>> APPLIED =
      new ConcurrentHashMap<>();

  /** 便携式可回滚句柄。 */
  public interface Applied {
    void revert();
  }

  /** 为任意生物应用一次 transient 属性修饰器，并在 TTL 到期时自动回滚。 若相同 ID 已存在，会先移除旧的再添加新的。 */
  public static Applied applyAttributeWithTTL(
      LivingEntity target,
      Holder<Attribute> attribute,
      ResourceLocation id,
      double amount,
      AttributeModifier.Operation op,
      int ttlTicks) {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(attribute, "attribute");
    Objects.requireNonNull(id, "id");

    Agents.applyTransientAttribute(target, attribute, id, amount, op);

    Applied handle = () -> Agents.removeAttribute(target, attribute, id);
    APPLIED.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>()).put(id, handle);

    if (ttlTicks > 0 && target.level() instanceof ServerLevel level) {
      int delay = Math.min(Integer.MAX_VALUE, ttlTicks);
      TickOps.schedule(level, () -> safeRevert(target.getUUID(), id), delay);
    }
    return handle;
  }

  /** 主动回滚某个实体上指定 ID 的属性修饰器。 */
  public static void revert(LivingEntity target, ResourceLocation id, Holder<Attribute> attribute) {
    if (target == null || id == null || attribute == null) return;
    safeRevert(target.getUUID(), id);
    Agents.removeAttribute(target, attribute, id);
  }

  /** 清理实体的全部临时属性修饰器（例如死亡/离线时机）。 */
  public static void cleanupAll(LivingEntity target) {
    if (target == null) return;
    Map<ResourceLocation, Applied> map = APPLIED.remove(target.getUUID());
    if (map == null || map.isEmpty()) return;
    map.values().forEach(AgentTempState::safeRun);
  }

  private static void safeRevert(UUID uuid, ResourceLocation id) {
    Map<ResourceLocation, Applied> map = APPLIED.get(uuid);
    if (map == null) return;
    Applied h = map.remove(id);
    safeRun(h);
  }

  private static void safeRun(Applied h) {
    try {
      if (h != null) h.revert();
    } catch (Throwable ignored) {
    }
  }
}
