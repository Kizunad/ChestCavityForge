package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning;

/**
 * Runtime-tuning constants for Hun Dao interfaces introduced in Phase 9.
 *
 * <p>These values intentionally act as placeholders so that later Phase 9.x drops can
 * customize cache duration and cooldown behavior without touching calculator code again.
 */
public final class HunDaoRuntimeTuning {

  private HunDaoRuntimeTuning() {}

  /** Tuning for Hun Dao scar (dao hen) caching strategies. */
  public static final class DaoHenCache {
    private DaoHenCache() {}

    /** Cache TTL in ticks for hun dao dao-hen lookups (placeholder: 20 ticks = 1 second). */
    public static final int TTL_TICKS = 20;

    /** Floating point tolerance while comparing cached dao-hen values. */
    public static final double EPSILON = 1e-6;
  }

  /** Tuning constants for Hun Dao cooldown scaling hooks. */
  public static final class Cooldown {
    private Cooldown() {}

    /**
     * Maximum cooldown reduction ratio. Phase 9 simply records usage, hence this stays at
     * zero until later balance drops implement real scaling.
     */
    public static final double MAX_REDUCTION = 0.0D;

    /** Default minimal cooldown (ticks) when scaling skills. */
    public static final long DEFAULT_MIN_TICKS = 20L;
  }

  /** Tuning constants for soul beast defensive scaling. */
  public static final class SoulBeastDefense {
    private SoulBeastDefense() {}

    /** Dao-hen softcap for damage mitigation scaling. */
    public static final double SCAR_SOFTCAP = 2000.0D;

    /** Maximum percentage of incoming damage that can be mitigated (0.0–1.0). */
    public static final double MAX_REDUCTION = 0.6D;
  }

  /** Tuning constants for Hun Po drain scaling. */
  public static final class HunPoDrain {
    private HunPoDrain() {}

    /** Maximum multiplier applied when Dao-hen is zero (faster drain). */
    public static final double MAX_MULTIPLIER = 1.5D;

    /** Minimum multiplier applied when Dao-hen reaches the softcap (slower drain). */
    public static final double MIN_MULTIPLIER = 0.5D;

    /** Threshold for detecting scar delta that should invalidate caches. */
    public static final double EPSILON = 1e-3D;
  }
}
