package net.tigereye.chestcavity.skill.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 极轻量“技能效果总线”：在技能 pre/post 时分发 Effect，并统一管理回滚与过期清理。
 */
public final class SkillEffectBus {

  private SkillEffectBus() {}

  private record PatternedEffect(Pattern pattern, Effect effect) {}

  private static final List<PatternedEffect> EFFECTS = new CopyOnWriteArrayList<>();

  public static void register(String regex, Effect effect) {
    if (effect == null) return;
    Pattern pattern = (regex == null || regex.isBlank()) ? null : Pattern.compile(regex);
    EFFECTS.add(new PatternedEffect(pattern, effect));
  }

  public static void pre(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry) {
    pre(player, skillId, cc, entry, null);
  }

  public static void pre(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry,
      EffectContext.UseItemInfo useItem) {
    if (player == null || skillId == null || cc == null || cc.inventory == null) return;
    if (EFFECTS.isEmpty()) return;
    long castId = SkillTempState.beginCast(player, skillId);
    EffectContext ctx =
        useItem == null
            ? EffectContext.build(player, skillId, cc, castId)
            : EffectContext.build(player, skillId, cc, castId, useItem);

    for (PatternedEffect e : EFFECTS) {
      if (!matches(e.pattern, skillId)) continue;
      try {
        AppliedHandle handle = e.effect.applyPre(ctx);
        if (handle != null) {
          SkillTempState.recordHandle(player, skillId, handle);
        }
      } catch (Throwable t) {
        ChestCavity.LOGGER.debug(
            "[SkillEffectBus][pre] effect failed for {}: {}", skillId, t.toString());
      }
    }
  }

  public static void post(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry,
      ActiveSkillRegistry.TriggerResult result) {
    post(player, skillId, cc, entry, result, null);
  }

  public static void post(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry,
      ActiveSkillRegistry.TriggerResult result,
      EffectContext.UseItemInfo useItem) {
    if (player == null || skillId == null || cc == null || cc.inventory == null) return;
    if (EFFECTS.isEmpty()) return;

    SkillTempState.PendingCast pending = SkillTempState.endCast(player, skillId);
    EffectContext ctx =
        useItem == null
            ? EffectContext.build(player, skillId, cc, pending.castId())
            : EffectContext.build(player, skillId, cc, pending.castId(), useItem);

    // 先让每个 Effect 做自己的后置逻辑（如成功才发药水、经验等）
    for (PatternedEffect e : EFFECTS) {
      if (!matches(e.pattern, skillId)) continue;
      try {
        e.effect.applyPost(ctx, result);
      } catch (Throwable t) {
        ChestCavity.LOGGER.debug(
            "[SkillEffectBus][post] effect failed for {}: {}", skillId, t.toString());
      }
    }

    // 清理或安排到期
    List<AppliedHandle> handles = new ArrayList<>(pending.handles());
    if (handles.isEmpty()) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      // 离线或非服务端环境，直接回滚
      for (AppliedHandle h : handles) safeRevert(h);
      return;
    }
    for (AppliedHandle h : handles) {
      if (result == ActiveSkillRegistry.TriggerResult.SUCCESS) {
        int ttl = Math.max(0, h.ttlTicks());
        if (ttl <= 0) {
          safeRevert(h);
        } else {
          // 到期清理：若 pre 阶段被其它流程提前回滚，revert() 应保证幂等
          TickOps.schedule(level, () -> safeRevert(h), ttl);
        }
      } else {
        safeRevert(h);
      }
    }
  }

  private static boolean matches(Pattern p, ResourceLocation id) {
    if (p == null || id == null) return true;
    return p.matcher(id.toString()).matches();
  }

  private static void safeRevert(AppliedHandle h) {
    try {
      h.revert();
    } catch (Throwable ignored) {
    }
  }

  // 运行期事件清理入口（离线/死亡/换维度）
  public static void cleanup(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    SkillTempState.cleanup(player);
  }

  public static void putMetadata(EffectContext ctx, String key, double value) {
    if (ctx == null || key == null) {
      return;
    }
    if (!(ctx.player() instanceof ServerPlayer player)) {
      return;
    }
    SkillTempState.putMetadata(player, ctx.skillId(), key, value);
  }

  public static double consumeMetadata(
      ServerPlayer player, ResourceLocation skillId, String key, double defaultValue) {
    return SkillTempState.consumeMetadata(player, skillId, key).orElse(defaultValue);
  }
}
