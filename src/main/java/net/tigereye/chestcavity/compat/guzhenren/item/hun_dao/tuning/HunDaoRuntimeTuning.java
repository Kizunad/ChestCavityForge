package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.HunDaoSoulLevelHelper;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.soul.HunDaoSoulLevelTier;

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

  /** Soul avatar general scaling knobs. */
  public static final class SoulAvatarScaling {
    private SoulAvatarScaling() {}

    /** Hun po required per +1x size/attack scale. */
    public static final double HUNPO_PER_SCALE_STEP = 100_000.0D;
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

  /** Tuning constants for mortal-shell (non-soul-beast) hun po limits. */
  public static final class MortalShell {
    private MortalShell() {}

    /** Base conversion ratio: every 1 HP safely carries this much hun po. */
    public static final double HUNPO_PER_HP = 1000.0D / 20.0D;

    /** Maximum person units allowed outside soul beast state (亿人魂). */
    public static final double MAX_PERSON_UNITS =
        HunDaoSoulLevelTier.HUNDRED_MILLION_PERSON.getMinPersonUnits();

    /** Hard upper bound for hun po softcap outside soul beast form. */
    public static final double ABSOLUTE_MAX_HUNPO =
        MAX_PERSON_UNITS * HunDaoSoulLevelHelper.HUNPO_PER_PERSON;

    /** Minimum leakage applied per second when above the softcap. */
    public static final double MIN_DECAY_PER_SECOND = 1.0D;

    /** Fraction of the excess capacity drained per second (softcap behaviour). */
    public static final double DECAY_FRACTION = 0.10D;

    /** Floating point tolerance for comparisons while clamping. */
    public static final double EPSILON = 1e-3D;
  }

  /** Tuning constants for soul avatar kill-leech hooks. */
  public static final class SoulAvatarKillLeech {
    private SoulAvatarKillLeech() {}

    /** Percentage of victim max HP converted into hun po on kill (0.001 = 0.1%). */
    public static final double HP_FRACTION = 0.001D;
  }

  /** Tuning constants for the unique hostile soul avatar boss. */
  public static final class SoulAvatarWorldBoss {
    private SoulAvatarWorldBoss() {}

    /** Initial hun po and capacity when spawned. */
    public static final double INITIAL_HUNPO = 10_000.0D;

    /** Max hun po gained per kill, based on victim HP. */
    public static final double MAX_HUNPO_PER_HP = 0.10D;

    /** Hun po (current) gained per point of victim hun po. */
    public static final double HUNPO_GAIN_MULTIPLIER = 1.0D;

    /** Cooldown between random teleports (ticks). */
    public static final long TELEPORT_COOLDOWN_TICKS = 200L;

    /** Scan radius when looking for low-HP prey to teleport onto. */
    public static final double TELEPORT_SCAN_RADIUS = 64.0D;

    /** Minimum horizontal spacing from reference players when spawning. */
    public static final double MIN_SPAWN_DISTANCE = 4000.0D;

    /** Additional randomized radius when selecting the spawn point. */
    public static final double EXTRA_SPAWN_DISTANCE = 512.0D;
  }
}
