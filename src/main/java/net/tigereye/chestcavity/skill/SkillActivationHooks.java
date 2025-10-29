package net.tigereye.chestcavity.skill;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * 统一管理技能触发的前/后置处理器。
 *
 * <p>允许通过 {@link java.util.regex.Pattern 正则} 过滤技能 ID，实现针对特定技能的扩展逻辑。</p>
 *
 * <p>线程安全：内部使用 CopyOnWriteArrayList 存储处理器，注册与触发并发安全。</p>
 */
public final class SkillActivationHooks {

  private SkillActivationHooks() {}

  private static final List<PatternedActivePreHandler> ACTIVE_PRE_HANDLERS =
      new CopyOnWriteArrayList<>();
  private static final List<PatternedActivePostHandler> ACTIVE_POST_HANDLERS =
      new CopyOnWriteArrayList<>();
  private static final List<PatternedComboPreHandler> COMBO_PRE_HANDLERS =
      new CopyOnWriteArrayList<>();
  private static final List<PatternedComboPostHandler> COMBO_POST_HANDLERS =
      new CopyOnWriteArrayList<>();

  /**
   * 注册主动技能触发前置处理器。
   *
   * @param regex 正则表达式；为 null 或空字符串时匹配所有 skillId
   * @param handler 处理器实例；为 null 时忽略
   */
  public static void registerActivePreHandler(String regex, ActivePreHandler handler) {
    if (handler == null) {
      return;
    }
    ACTIVE_PRE_HANDLERS.add(
        new PatternedActivePreHandler(compile(regex), Objects.requireNonNull(handler)));
  }

  /**
   * 注册主动技能触发后置处理器。
   *
   * @param regex 正则表达式；为 null 或空字符串时匹配所有 skillId
   * @param handler 处理器实例；为 null 时忽略
   */
  public static void registerActivePostHandler(String regex, ActivePostHandler handler) {
    if (handler == null) {
      return;
    }
    ACTIVE_POST_HANDLERS.add(
        new PatternedActivePostHandler(compile(regex), Objects.requireNonNull(handler)));
  }

  /**
   * 注册组合杀招触发前置处理器。
   *
   * @param regex 正则表达式；为 null 或空字符串时匹配所有 skillId
   * @param handler 处理器实例；为 null 时忽略
   */
  public static void registerComboPreHandler(String regex, ComboPreHandler handler) {
    if (handler == null) {
      return;
    }
    COMBO_PRE_HANDLERS.add(
        new PatternedComboPreHandler(compile(regex), Objects.requireNonNull(handler)));
  }

  /**
   * 注册组合杀招触发后置处理器。
   *
   * @param regex 正则表达式；为 null 或空字符串时匹配所有 skillId
   * @param handler 处理器实例；为 null 时忽略
   */
  public static void registerComboPostHandler(String regex, ComboPostHandler handler) {
    if (handler == null) {
      return;
    }
    COMBO_POST_HANDLERS.add(
        new PatternedComboPostHandler(compile(regex), Objects.requireNonNull(handler)));
  }

  /**
   * 触发主动技能前置处理器链。
   *
   * @param player 服务器玩家（可能为 null）
   * @param skillId 技能 ID
   * @param cc 胸腔实例（可能为 null）
   * @param entry 主动技能注册条目
   * @return 处理器综合决策，默认继续
   */
  static ActivePreHookDecision fireActivePreHandlers(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry) {
    for (PatternedActivePreHandler registration : ACTIVE_PRE_HANDLERS) {
      if (!matches(registration.pattern(), skillId)) {
        continue;
      }
      ActivePreHookDecision decision =
          registration.handler().beforeActivate(player, skillId, cc, entry);
      if (decision != null && !decision.shouldProceed()) {
        return decision;
      }
    }
    return ActivePreHookDecision.continueChain();
  }

  /**
   * 触发主动技能后置处理器链。
   *
   * @param player 服务器玩家（可能为 null）
   * @param skillId 技能 ID
   * @param cc 胸腔实例（可能为 null）
   * @param entry 主动技能注册条目
   * @param result 激活结果
   */
  static void fireActivePostHandlers(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ActiveSkillRegistry.ActiveSkillEntry entry,
      ActiveSkillRegistry.TriggerResult result) {
    for (PatternedActivePostHandler registration : ACTIVE_POST_HANDLERS) {
      if (!matches(registration.pattern(), skillId)) {
        continue;
      }
      registration.handler().afterActivate(player, skillId, cc, entry, result);
    }
  }

  /**
   * 触发组合杀招前置处理器链。
   *
   * @param player 服务器玩家（可能为 null）
   * @param skillId 技能 ID
   * @param cc 胸腔实例（可能为 null）
   * @param entry 组合杀招注册条目
   * @return 处理器综合决策，默认继续
   */
  static ComboPreHookDecision fireComboPreHandlers(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ComboSkillRegistry.ComboSkillEntry entry) {
    for (PatternedComboPreHandler registration : COMBO_PRE_HANDLERS) {
      if (!matches(registration.pattern(), skillId)) {
        continue;
      }
      ComboPreHookDecision decision =
          registration.handler().beforeActivate(player, skillId, cc, entry);
      if (decision != null && !decision.shouldProceed()) {
        return decision;
      }
    }
    return ComboPreHookDecision.continueChain();
  }

  /**
   * 触发组合杀招后置处理器链。
   *
   * @param player 服务器玩家（可能为 null）
   * @param skillId 技能 ID
   * @param cc 胸腔实例（可能为 null）
   * @param entry 组合杀招注册条目
   * @param result 激活结果
   */
  static void fireComboPostHandlers(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      ComboSkillRegistry.ComboSkillEntry entry,
      ComboSkillRegistry.TriggerResult result) {
    for (PatternedComboPostHandler registration : COMBO_POST_HANDLERS) {
      if (!matches(registration.pattern(), skillId)) {
        continue;
      }
      registration.handler().afterActivate(player, skillId, cc, entry, result);
    }
  }

  private static Pattern compile(String regex) {
    if (regex == null || regex.isBlank()) {
      return null;
    }
    return Pattern.compile(regex);
  }

  private static boolean matches(Pattern pattern, ResourceLocation skillId) {
    if (pattern == null || skillId == null) {
      return true;
    }
    return pattern.matcher(skillId.toString()).matches();
  }

  /**
   * 测试辅助：清空所有处理器注册。
   */
  static void clearAllHandlers() {
    ACTIVE_PRE_HANDLERS.clear();
    ACTIVE_POST_HANDLERS.clear();
    COMBO_PRE_HANDLERS.clear();
    COMBO_POST_HANDLERS.clear();
  }

  private record PatternedActivePreHandler(
      Pattern pattern, ActivePreHandler handler) {}

  private record PatternedActivePostHandler(
      Pattern pattern, ActivePostHandler handler) {}

  private record PatternedComboPreHandler(
      Pattern pattern, ComboPreHandler handler) {}

  private record PatternedComboPostHandler(
      Pattern pattern, ComboPostHandler handler) {}

  /**
   * 主动技能前置处理器。
   */
  @FunctionalInterface
  public interface ActivePreHandler {
    /**
     * 主动技能触发前调用。
     *
     * @param player 服务器玩家（可能为 null）
     * @param skillId 技能 ID
     * @param cc 胸腔实例（可能为 null）
     * @param entry 主动技能注册条目
     * @return 决策对象；返回取消可阻断后续处理与实际触发
     */
    ActivePreHookDecision beforeActivate(
        ServerPlayer player,
        ResourceLocation skillId,
        ChestCavityInstance cc,
        ActiveSkillRegistry.ActiveSkillEntry entry);
  }

  /**
   * 主动技能后置处理器。
   */
  @FunctionalInterface
  public interface ActivePostHandler {
    /**
     * 主动技能触发完成后调用（无论成功或失败）。
     *
     * @param player 服务器玩家（可能为 null）
     * @param skillId 技能 ID
     * @param cc 胸腔实例（可能为 null）
     * @param entry 主动技能注册条目
     * @param result 触发结果
     */
    void afterActivate(
        ServerPlayer player,
        ResourceLocation skillId,
        ChestCavityInstance cc,
        ActiveSkillRegistry.ActiveSkillEntry entry,
        ActiveSkillRegistry.TriggerResult result);
  }

  /**
   * 组合杀招前置处理器。
   */
  @FunctionalInterface
  public interface ComboPreHandler {
    /**
     * 组合杀招触发前调用。
     *
     * @param player 服务器玩家（可能为 null）
     * @param skillId 技能 ID
     * @param cc 胸腔实例（可能为 null）
     * @param entry 组合杀招注册条目
     * @return 决策对象；返回取消可阻断后续处理与实际触发
     */
    ComboPreHookDecision beforeActivate(
        ServerPlayer player,
        ResourceLocation skillId,
        ChestCavityInstance cc,
        ComboSkillRegistry.ComboSkillEntry entry);
  }

  /**
   * 组合杀招后置处理器。
   */
  @FunctionalInterface
  public interface ComboPostHandler {
    /**
     * 组合杀招触发完成后调用（无论成功或失败）。
     *
     * @param player 服务器玩家（可能为 null）
     * @param skillId 技能 ID
     * @param cc 胸腔实例（可能为 null）
     * @param entry 组合杀招注册条目
     * @param result 触发结果
     */
    void afterActivate(
        ServerPlayer player,
        ResourceLocation skillId,
        ChestCavityInstance cc,
        ComboSkillRegistry.ComboSkillEntry entry,
        ComboSkillRegistry.TriggerResult result);
  }

  /**
   * 主动技能触发前置处理结果。
   *
   * @param shouldProceed 是否继续后续处理及实际激活
   * @param overrideResult 如果拦截，返回的替代结果
   */
  public record ActivePreHookDecision(
      boolean shouldProceed, ActiveSkillRegistry.TriggerResult overrideResult) {

    public ActivePreHookDecision {
      if (!shouldProceed && overrideResult == null) {
        throw new IllegalArgumentException("overrideResult must be provided when blocking.");
      }
    }

    /**
     * 继续后续处理与实际激活。
     * @return 决策对象
     */
    public static ActivePreHookDecision continueChain() {
      return new ActivePreHookDecision(true, null);
    }

    /**
     * 取消后续处理与实际激活，并返回替代结果。
     * @param overrideResult 替代触发结果
     * @return 决策对象
     */
    public static ActivePreHookDecision cancel(
        ActiveSkillRegistry.TriggerResult overrideResult) {
      return new ActivePreHookDecision(false, overrideResult);
    }

    /**
     * 是否被拦截。
     * @return true 表示取消
     */
    public boolean blocked() {
      return !shouldProceed;
    }
  }

  /**
   * 组合杀招触发前置处理结果。
   *
   * @param shouldProceed 是否继续后续处理及实际激活
   * @param overrideResult 如果拦截，返回的替代结果
   */
  public record ComboPreHookDecision(
      boolean shouldProceed, ComboSkillRegistry.TriggerResult overrideResult) {

    public ComboPreHookDecision {
      if (!shouldProceed && overrideResult == null) {
        throw new IllegalArgumentException("overrideResult must be provided when blocking.");
      }
    }

    /**
     * 继续后续处理与实际激活。
     * @return 决策对象
     */
    public static ComboPreHookDecision continueChain() {
      return new ComboPreHookDecision(true, null);
    }

    /**
     * 取消后续处理与实际激活，并返回替代结果。
     * @param overrideResult 替代触发结果
     * @return 决策对象
     */
    public static ComboPreHookDecision cancel(
        ComboSkillRegistry.TriggerResult overrideResult) {
      return new ComboPreHookDecision(false, overrideResult);
    }

    /**
     * 是否被拦截。
     * @return true 表示取消
     */
    public boolean blocked() {
      return !shouldProceed;
    }
  }
}
