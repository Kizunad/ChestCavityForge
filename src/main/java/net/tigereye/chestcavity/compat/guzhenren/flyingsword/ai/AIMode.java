package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai;

/** 飞剑AI模式 */
public enum AIMode {
  /** 环绕模式：待命跟随 */
  ORBIT("orbit", "环绕"),

  /** 防守模式：主人受击后锁定目标 */
  GUARD("guard", "防守"),

  /** 出击模式：主动扫描并攻击目标 */
  HUNT("hunt", "出击"),

  /** 悬浮模式：靠近主人并在近身处悬停 */
  HOVER("hover", "悬浮"),

  /** 召回模式：弧形轨迹返回主人 */
  RECALL("recall", "召回"),

  /** 集群模式：由集群管理器统一调度（青莲剑群专用） */
  SWARM("swarm", "集群");

  private final String id;
  private final String displayName;

  AIMode(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static AIMode fromId(String id) {
    for (AIMode mode : values()) {
      if (mode.id.equals(id)) {
        return mode;
      }
    }
    return ORBIT; // 默认返回环绕模式
  }

  public static AIMode fromOrdinal(int ordinal) {
    if (ordinal >= 0 && ordinal < values().length) {
      return values()[ordinal];
    }
    return ORBIT;
  }
}
