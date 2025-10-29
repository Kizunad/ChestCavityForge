package net.tigereye.chestcavity.registration;

import net.tigereye.chestcavity.skill.SkillActivationHooks;

/**
 * 统一注册所有 SkillActivationHooks 扩展，避免在具体工具类内散落静态初始化逻辑。
 */
public final class ActivationHookRegistry {

  private static boolean initialised = false;

  private ActivationHookRegistry() {}

  /** 注册所有技能触发 Hook。 */
  public static void register() {
    if (initialised) {
      return;
    }
    initialised = true;

    SkillActivationHooks.registerActivePostHandler(
        "^guzhenren:.*$", GuzhenrenFlowActivationHooks::handleSkillPostActivation);
  }
}
