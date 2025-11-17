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

  /** Tuning constants for 鬼气蛊 (Gui Qi Gu) mechanics. */
  public static final class GuiQiGu {
    private GuiQiGu() {}

    /** Passive hunpo recovery per second. */
    public static final double PASSIVE_HUNPO_PER_SECOND = 3.0D;

    /** Passive jingli recovery per second. */
    public static final double PASSIVE_JINGLI_PER_SECOND = 1.0D;

    /** True damage ratio based on max hunpo (3% of max hunpo). */
    public static final double TRUE_DAMAGE_RATIO = 0.03D;

    /** Gui Wu (鬼雾) effect radius. */
    public static final double GUI_WU_RADIUS = 4.0D;
  }

  /** Tuning constants for 体魄蛊 (Ti Po Gu) mechanics. */
  public static final class TiPoGu {
    private TiPoGu() {}

    /** Passive hunpo recovery per second. */
    public static final double PASSIVE_HUNPO_PER_SECOND = 3.0D;

    /** Passive jingli recovery per second. */
    public static final double PASSIVE_JINGLI_PER_SECOND = 1.0D;

    /** Soul beast mode damage percent (3% of max hunpo). */
    public static final double SOUL_BEAST_DAMAGE_PERCENT = 0.03D;

    /** Soul beast hunpo cost percent (0.1% of max hunpo). */
    public static final double SOUL_BEAST_HUNPO_COST_PERCENT = 0.001D;

    /** Zi Hun (子魂) increase effect bonus (10%). */
    public static final double ZI_HUN_INCREASE_BONUS = 0.10D;

    /** Shield percent based on max hunpo (0.5%). */
    public static final double SHIELD_PERCENT = 0.005D;
  }
}
