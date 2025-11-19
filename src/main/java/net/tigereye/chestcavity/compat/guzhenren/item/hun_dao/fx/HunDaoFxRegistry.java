package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;

/**
 * Registry for Hun Dao FX templates.
 *
 * <p>Stores metadata for each FX type (sound events, particle templates, duration parameters) and
 * provides lookup by FX ID. Used by HunDaoFxRouter to dispatch effects in a data-driven manner.
 *
 * <p>Phase 5: Centralized FX registration system.
 */
public final class HunDaoFxRegistry {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<ResourceLocation, FxTemplate> REGISTRY = new HashMap<>();

  private HunDaoFxRegistry() {}

  /**
   * Registers an FX template.
   *
   * @param fxId the FX resource location
   * @param template the FX template metadata
   */
  public static void register(ResourceLocation fxId, FxTemplate template) {
    Objects.requireNonNull(fxId, "fxId cannot be null");
    Objects.requireNonNull(template, "template cannot be null");

    if (REGISTRY.containsKey(fxId)) {
      LOGGER.warn("[hun_dao][fx_registry] Overwriting existing FX template: {}", fxId);
    }

    REGISTRY.put(fxId, template);
    LOGGER.debug(
        "[hun_dao][fx_registry] Registered FX: {} (sound={}, continuous={})",
        fxId,
        template.hasSound(),
        template.continuous);
  }

  /**
   * Retrieves an FX template by ID.
   *
   * @param fxId the FX resource location
   * @return the FX template, or null if not registered
   */
  public static FxTemplate get(ResourceLocation fxId) {
    return REGISTRY.get(fxId);
  }

  /**
   * Checks if an FX ID is registered.
   *
   * @param fxId the FX resource location
   * @return true if registered, false otherwise
   */
  public static boolean isRegistered(ResourceLocation fxId) {
    return REGISTRY.containsKey(fxId);
  }

  /**
   * Returns the number of registered FX templates.
   *
   * @return the registry size
   */
  public static int size() {
    return REGISTRY.size();
  }

  /**
   * Clears all registered FX templates.
   *
   * <p>Used for testing or hot-reloading scenarios.
   */
  public static void clear() {
    LOGGER.info("[hun_dao][fx_registry] Clearing {} FX templates", REGISTRY.size());
    REGISTRY.clear();
  }

  /**
   * FX template metadata.
   *
   * <p>Stores sound event, particle template reference, and behavioral flags for each FX type.
   * Instances are created via builder pattern for flexibility.
   */
  public static final class FxTemplate {
    /** Optional sound event to play when FX is triggered. */
    public final Supplier<SoundEvent> soundSupplier;

    /** Sound volume (0.0 to 1.0+). */
    public final float soundVolume;

    /** Sound pitch (typically 0.5 to 2.0). */
    public final float soundPitch;

    /** Whether this FX is continuous (ambient) or one-shot (burst). */
    public final boolean continuous;

    /** Default duration in ticks (for continuous effects). */
    public final int durationTicks;

    /** Minimum ticks between repeated sound playback for the same anchor. */
    public final int minRepeatIntervalTicks;

    /** Optional particle template identifier (for FxEngine integration). */
    public final String particleTemplate;

    /** Optional FX category for filtering. */
    public final HunDaoFxDescriptors.FxCategory category;

    private FxTemplate(Builder builder) {
      this.soundSupplier = builder.soundSupplier;
      this.soundVolume = builder.soundVolume;
      this.soundPitch = builder.soundPitch;
      this.continuous = builder.continuous;
      this.durationTicks = builder.durationTicks;
      this.particleTemplate = builder.particleTemplate;
      this.category = builder.category;
      this.minRepeatIntervalTicks = builder.minRepeatIntervalTicks;
    }

    /**
     * Creates a new FxTemplate builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
      return new Builder();
    }

    /**
     * Builder for creating FxTemplate instances.
     *
     * <p>Provides a fluent API for configuring FX template properties.
     */
    public static final class Builder {
      private Supplier<SoundEvent> soundSupplier;
      private float soundVolume = 1.0F;
      private float soundPitch = 1.0F;
      private boolean continuous = false;
      private int durationTicks = 100;
      private String particleTemplate;
      private HunDaoFxDescriptors.FxCategory category = HunDaoFxDescriptors.FxCategory.UTILITY;
      private int minRepeatIntervalTicks = 0;

      /**
       * Configures the sound for this FX.
       *
       * @param soundSupplier supplier for the SoundEvent
       * @param volume sound volume
       * @param pitch sound pitch
       * @return this builder
       */
      public Builder sound(Supplier<SoundEvent> soundSupplier, float volume, float pitch) {
        this.soundSupplier = soundSupplier;
        this.soundVolume = volume;
        this.soundPitch = pitch;
        return this;
      }

      /**
       * Marks this FX as continuous.
       *
       * @param continuous true for continuous, false for one-shot
       * @return this builder
       */
      public Builder continuous(boolean continuous) {
        this.continuous = continuous;
        return this;
      }

      /**
       * Sets the duration for continuous FX.
       *
       * @param ticks duration in ticks
       * @return this builder
       */
      public Builder duration(int ticks) {
        this.durationTicks = ticks;
        return this;
      }

      /**
       * Sets the particle template for this FX.
       *
       * @param template particle template name
       * @return this builder
       */
      public Builder particles(String template) {
        this.particleTemplate = template;
        return this;
      }

      /**
       * Sets the category for this FX.
       *
       * @param category the FX category
       * @return this builder
       */
      public Builder category(HunDaoFxDescriptors.FxCategory category) {
        this.category = category;
        return this;
      }

      /**
       * Sets the minimum interval between sound repeats.
       *
       * @param ticks interval in ticks
       * @return this builder
       */
      public Builder minRepeatIntervalTicks(int ticks) {
        this.minRepeatIntervalTicks = Math.max(0, ticks);
        return this;
      }

      /**
       * Builds the FxTemplate.
       *
       * @return a new FxTemplate instance
       */
      public FxTemplate build() {
        return new FxTemplate(this);
      }
    }

    /** Returns true if this template configured a sound supplier. */
    public boolean hasSound() {
      return soundSupplier != null;
    }

    /** Resolves the configured sound event, or null if unavailable. */
    public SoundEvent resolveSound() {
      return soundSupplier != null ? soundSupplier.get() : null;
    }

    /** Returns the minimum repeat interval (ticks) for the configured sound. */
    public int minRepeatIntervalTicks() {
      return minRepeatIntervalTicks;
    }
  }
}
