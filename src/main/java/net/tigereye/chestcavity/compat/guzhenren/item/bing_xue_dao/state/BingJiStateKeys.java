package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.state;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/** 冰肌蛊：统一的状态键与 MultiCooldown 读写帮助 */
public final class BingJiStateKeys {
  private BingJiStateKeys() {}

  // 兼容旧键：仍保留占位（如重建或清理时需要）
  public static final String ABSORPTION_TIMER_KEY = "AbsorptionTimer"; // legacy int (unused)

  // 统一使用 readyAt 时间戳
  public static final String ABSORPTION_READY_AT_KEY = "AbsorptionReadyAt";
  public static final String INVULN_READY_AT_KEY = "InvulnReadyAt";

  public static MultiCooldown.Entry absorptionEntry(MultiCooldown cd) {
    return cd.entry(ABSORPTION_READY_AT_KEY);
  }

  public static MultiCooldown.Entry invulnEntry(MultiCooldown cd) {
    return cd.entry(INVULN_READY_AT_KEY);
  }
}

