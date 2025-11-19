package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.tuning.HunDaoFxTuning;
import net.tigereye.chestcavity.registration.CCSoundEvents;

/**
 * Initializes all Hun Dao FX templates on game startup.
 *
 * <p>Registers soul flame, soul beast, gui wu, and hun po effect templates with HunDaoFxRegistry.
 * This class should be called during mod initialization to ensure all FX are available when needed.
 *
 * <p>Phase 5: Data-driven FX system initialization.
 */
public final class HunDaoFxInit {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static boolean initialized = false;

  private HunDaoFxInit() {}

  /**
   * Registers all Hun Dao FX templates.
   *
   * <p>Safe to call multiple times - subsequent calls are ignored.
   */
  public static void init() {
    if (initialized) {
      LOGGER.debug("[hun_dao][fx_init] Already initialized, skipping");
      return;
    }

    LOGGER.info("[hun_dao][fx_init] Registering Hun Dao FX templates...");

    registerSoulFlameFx();
    registerSoulBeastFx();
    registerGuiWuFx();
    registerHunPoFx();

    initialized = true;
    LOGGER.info("[hun_dao][fx_init] Registered {} FX templates", HunDaoFxRegistry.size());
  }

  /** Registers soul flame (魂焰) effect templates. */
  private static void registerSoulFlameFx() {
    // Soul flame DoT tick (continuous effect while DoT is active)
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_FLAME_TICK,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT,
                HunDaoFxTuning.SoulFlame.SOUND_VOLUME,
                HunDaoFxTuning.SoulFlame.SOUND_PITCH)
            .continuous(true)
            .duration(HunDaoFxTuning.SoulFlame.PARTICLE_INTERVAL_TICKS)
            .minRepeatIntervalTicks(HunDaoFxTuning.SoulFlame.SOUND_REPEAT_INTERVAL_TICKS)
            .particles("soul_flame_tick")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_FLAME)
            .build());

    // Soul flame ignition (one-shot burst when first applied)
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_FLAME_IGNITE,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT,
                HunDaoFxTuning.SoulFlame.SOUND_VOLUME,
                HunDaoFxTuning.SoulFlame.SOUND_PITCH)
            .continuous(false)
            .particles("soul_flame_ignite")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_FLAME)
            .build());

    // Soul flame expiration (when DoT ends)
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_FLAME_EXPIRE,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(false)
            .particles("soul_flame_expire")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_FLAME)
            .build());
  }

  /** Registers soul beast (魂兽化) effect templates. */
  private static void registerSoulBeastFx() {
    // Soul beast activation transformation
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_BEAST_ACTIVATE,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT, // Reuse for now, can add dedicated sound later
                HunDaoFxTuning.SoulBeast.ACTIVATION_SOUND_VOLUME,
                HunDaoFxTuning.SoulBeast.ACTIVATION_SOUND_PITCH)
            .continuous(false)
            .particles("soul_beast_activate")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_BEAST)
            .build());

    // Soul beast ambient aura (continuous while active)
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_BEAST_AMBIENT,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(true)
            .duration(HunDaoFxTuning.SoulBeast.AMBIENT_INTERVAL_TICKS)
            .particles("soul_beast_ambient")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_BEAST)
            .build());

    // Soul beast deactivation
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_BEAST_DEACTIVATE,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT,
                HunDaoFxTuning.SoulBeast.DEACTIVATION_SOUND_VOLUME,
                HunDaoFxTuning.SoulBeast.DEACTIVATION_SOUND_PITCH)
            .continuous(false)
            .particles("soul_beast_deactivate")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_BEAST)
            .build());

    // Soul beast melee hit impact
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.SOUL_BEAST_HIT,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(false)
            .particles("soul_beast_hit")
            .category(HunDaoFxDescriptors.FxCategory.SOUL_BEAST)
            .build());
  }

  /** Registers gui wu (鬼雾) effect templates. */
  private static void registerGuiWuFx() {
    // Gui wu activation
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.GUI_WU_ACTIVATE,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT, // Placeholder, can add dedicated sound
                HunDaoFxTuning.GuiWu.ACTIVATION_SOUND_VOLUME,
                HunDaoFxTuning.GuiWu.ACTIVATION_SOUND_PITCH)
            .continuous(false)
            .particles("gui_wu_activate")
            .category(HunDaoFxDescriptors.FxCategory.GUI_WU)
            .build());

    // Gui wu ambient fog (continuous)
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.GUI_WU_AMBIENT,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(true)
            .duration(HunDaoFxTuning.GuiWu.PARTICLE_INTERVAL_TICKS)
            .particles("gui_wu_ambient")
            .category(HunDaoFxDescriptors.FxCategory.GUI_WU)
            .build());

    // Gui wu dissipation
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.GUI_WU_DISSIPATE,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(false)
            .particles("gui_wu_dissipate")
            .category(HunDaoFxDescriptors.FxCategory.GUI_WU)
            .build());
  }

  /** Registers hun po (魂魄) resource effect templates. */
  private static void registerHunPoFx() {
    // Hun po leak/drain particles
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.HUN_PO_LEAK,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(true)
            .duration(20) // 1 second intervals
            .particles("hun_po_leak")
            .category(HunDaoFxDescriptors.FxCategory.HUN_PO)
            .build());

    // Hun po recovery burst
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.HUN_PO_RECOVERY,
        HunDaoFxRegistry.FxTemplate.builder()
            .continuous(false)
            .particles("hun_po_recovery")
            .category(HunDaoFxDescriptors.FxCategory.HUN_PO)
            .build());

    // Hun po low warning
    HunDaoFxRegistry.register(
        HunDaoFxDescriptors.HUN_PO_LOW_WARNING,
        HunDaoFxRegistry.FxTemplate.builder()
            .sound(
                CCSoundEvents.CUSTOM_SOULBEAST_DOT,
                HunDaoFxTuning.HunPoDrain.LEAK_WARNING_VOLUME,
                HunDaoFxTuning.HunPoDrain.LEAK_WARNING_PITCH)
            .continuous(false)
            .particles("hun_po_low_warning")
            .category(HunDaoFxDescriptors.FxCategory.HUN_PO)
            .build());
  }
}
