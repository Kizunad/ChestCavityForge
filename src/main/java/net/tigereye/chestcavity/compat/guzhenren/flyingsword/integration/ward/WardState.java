package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

/**
 * 护幕飞剑的状态机
 *
 * <p>护幕飞剑在不同状态间切换，以实现拦截和反击功能：
 *
 * <ul>
 *   <li>{@link #ORBIT} - 环绕主人，待命状态
 *   <li>{@link #INTERCEPT} - 向拦截点移动，时间窗口内（0.1-1.0秒）
 *   <li>{@link #COUNTER} - 反击完成，仅当距离 ≤ 5m 时触发
 *   <li>{@link #RETURN} - 返回环绕位置
 * </ul>
 *
 * 状态转换流程：
 *
 * <pre>
 * ORBIT → INTERCEPT → COUNTER/RETURN → ORBIT
 * </pre>
 */
public enum WardState {
  /**
   * 环绕主人（待命态）
   *
   * <p>飞剑在主人周围固定槽位环绕，等待拦截任务
   */
  ORBIT("orbit", "环绕"),

  /**
   * 向拦截点移动（可达窗口内，2-20 tick）
   *
   * <p>飞剑已接受拦截任务，正在向预测的拦截点 P* 移动
   */
  INTERCEPT("intercept", "拦截"),

  /**
   * 反击完成（仅当距离 ≤ 5m 时触发）
   *
   * <p>成功拦截后，若攻击者距离在反击范围内，执行反击
   */
  COUNTER("counter", "反击"),

  /**
   * 返回环绕位置
   *
   * <p>拦截完成或失败后，返回到原来的环绕槽位
   */
  RETURN("return", "返回");

  private final String id;
  private final String displayName;

  /**
   * 构造枚举值
   *
   * @param id 状态标识符（用于序列化）
   * @param displayName 显示名称（用于UI/日志）
   */
  WardState(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  /**
   * 获取状态标识符
   *
   * @return 状态ID
   */
  public String getId() {
    return id;
  }

  /**
   * 获取显示名称
   *
   * @return 显示名称
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * 从字符串ID解析状态
   *
   * @param id 状态标识符
   * @return 对应的状态枚举，如果不存在则返回 {@link #ORBIT}
   */
  public static WardState fromId(String id) {
    if (id == null) return ORBIT;
    for (WardState s : values()) {
      if (s.id.equals(id)) return s;
    }
    return ORBIT;
  }
}
