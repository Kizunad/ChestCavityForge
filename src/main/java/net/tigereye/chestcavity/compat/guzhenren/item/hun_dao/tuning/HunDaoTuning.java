package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning;

/**
 * Central tuning constants for Hun Dao (Soul Path) mechanics.
 *
 * <p>This replaces the legacy HunDaoBalance class and organizes constants by functional area to
 * improve maintainability and alignment with the jian_dao architecture.
 *
 * <p>Migrated from HunDaoBalance during Phase 1 of the Hun Dao rearchitecture.
 */
public final class HunDaoTuning {

  private HunDaoTuning() {}

  /** Tuning constants for soul beast (魂兽化) mechanics. */
  public static final class SoulBeast {
    private SoulBeast() {}

    /** Passive hunpo leak rate per second while soul beast is active. */
    public static final double HUNPO_LEAK_PER_SEC = 3.0D;

    /** Hunpo cost per melee hit when applying soul flame. */
    public static final double ON_HIT_COST = 18.0D;
  }

  /** Tuning constants for soul flame (魂焰) DoT mechanics. */
  public static final class SoulFlame {
    private SoulFlame() {}

    /** Soul flame damage per second as a factor of max hunpo. */
    public static final double DPS_FACTOR = 0.01D;

    /** Default soul flame duration in seconds. */
    public static final int DURATION_SECONDS = 5;
  }

  /** Tuning constants for 小魂蛊 (Xiao Hun Gu) mechanics. */
  public static final class XiaoHunGu {
    private XiaoHunGu() {}

    /** Base hunpo recovery amount. */
    public static final double RECOVER = 1.0D;

    /** Bonus recovery multiplier. */
    public static final double RECOVER_BONUS = 0.2D;
  }

  /** Tuning constants for 大魂蛊 (Da Hun Gu) mechanics. */
  public static final class DaHunGu {
    private DaHunGu() {}

    /** Hunpo recovery amount. */
    public static final double RECOVER = 2.0D;

    /** Niantou (念头) generation amount. */
    public static final double NIANTOU = 1.0D;
  }

  /** Tuning constants for general Hun Dao effects. */
  public static final class Effects {
    private Effects() {}

    /** Deterrence effect radius. */
    public static final double DETER_RADIUS = 8.0D;
  }
}
