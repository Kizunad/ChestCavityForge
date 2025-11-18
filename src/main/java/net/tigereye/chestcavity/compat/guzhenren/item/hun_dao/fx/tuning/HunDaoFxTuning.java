package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.tuning;

/**
 * FX tuning parameters for Hun Dao visual and audio effects.
 *
 * <p>Centralizes all FX-related configuration (durations, volumes, colors, particle counts) to
 * enable data-driven effect dispatch. Follows KISS principle - only includes parameters for
 * existing Hun Dao features.
 *
 * <p>Phase 5: FX/Client/UI decoupling from server middleware.
 */
public final class HunDaoFxTuning {

  private HunDaoFxTuning() {}

  /** Soul Flame (魂焰) DoT effect parameters. */
  public static final class SoulFlame {
    private SoulFlame() {}

    /** Sound volume for soul flame ignition. */
    public static final float SOUND_VOLUME = 0.6F;

    /** Sound pitch for soul flame ignition. */
    public static final float SOUND_PITCH = 1.0F;

    /** Default particle count per emission cycle. */
    public static final int PARTICLE_COUNT = 8;

    /** Particle emission interval in ticks. */
    public static final int PARTICLE_INTERVAL_TICKS = 5;

    /** Particle ring count for periodic burst effects. */
    public static final int RING_PARTICLE_COUNT = 12;

    /** Ring particle spawn interval in ticks. */
    public static final int RING_INTERVAL_TICKS = 10;

    /** Base particle radius around target. */
    public static final double PARTICLE_RADIUS = 0.5D;

    /** Ring particle radius around target. */
    public static final double RING_RADIUS = 0.7D;

    /** Minimum interval between DOT sound replays (ticks). */
    public static final int SOUND_REPEAT_INTERVAL_TICKS = 40;
  }

  /** Soul Beast (魂兽化) transformation effect parameters. */
  public static final class SoulBeast {
    private SoulBeast() {}

    /** Sound volume for soul beast activation. */
    public static final float ACTIVATION_SOUND_VOLUME = 0.8F;

    /** Sound pitch for soul beast activation. */
    public static final float ACTIVATION_SOUND_PITCH = 1.0F;

    /** Sound volume for soul beast deactivation. */
    public static final float DEACTIVATION_SOUND_VOLUME = 0.6F;

    /** Sound pitch for soul beast deactivation. */
    public static final float DEACTIVATION_SOUND_PITCH = 0.9F;

    /** Particle count for transformation burst. */
    public static final int TRANSFORM_PARTICLE_COUNT = 30;

    /** Ambient particle count while soul beast is active. */
    public static final int AMBIENT_PARTICLE_COUNT = 3;

    /** Ambient particle emission interval in ticks. */
    public static final int AMBIENT_INTERVAL_TICKS = 5;
  }

  /** Gui Wu (鬼雾) fog effect parameters. */
  public static final class GuiWu {
    private GuiWu() {}

    /** Sound volume for gui wu activation. */
    public static final float ACTIVATION_SOUND_VOLUME = 0.7F;

    /** Sound pitch for gui wu activation. */
    public static final float ACTIVATION_SOUND_PITCH = 0.8F;

    /** Particle density (particles per block in radius). */
    public static final int PARTICLE_DENSITY = 5;

    /** Particle emission interval in ticks. */
    public static final int PARTICLE_INTERVAL_TICKS = 3;

    /** Vertical particle spread range. */
    public static final double VERTICAL_SPREAD = 2.0D;

    /** Fog particle movement speed. */
    public static final double PARTICLE_SPEED = 0.02D;
  }

  /** Hun Po (魂魄) drain/leak effect parameters. */
  public static final class HunPoDrain {
    private HunPoDrain() {}

    /** Particle count per drain tick. */
    public static final int PARTICLE_COUNT = 2;

    /** Drain effect color tint (R). */
    public static final float COLOR_R = 0.3F;

    /** Drain effect color tint (G). */
    public static final float COLOR_G = 0.6F;

    /** Drain effect color tint (B). */
    public static final float COLOR_B = 0.9F;

    /** Sound volume for hun po leak warning. */
    public static final float LEAK_WARNING_VOLUME = 0.4F;

    /** Sound pitch for hun po leak warning. */
    public static final float LEAK_WARNING_PITCH = 1.2F;
  }

  /** General FX constants. */
  public static final class General {
    private General() {}

    /** Default FX duration fallback in seconds. */
    public static final int DEFAULT_DURATION_SECONDS = 5;

    /** Ticks per second conversion constant. */
    public static final int TICKS_PER_SECOND = 20;

    /** Maximum FX distance for client rendering (blocks). */
    public static final double MAX_RENDER_DISTANCE = 64.0D;
  }
}
