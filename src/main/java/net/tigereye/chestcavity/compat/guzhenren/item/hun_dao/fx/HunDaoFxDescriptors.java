package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Declares all available Hun Dao FX IDs and their metadata.
 *
 * <p>Provides centralized registry of effect descriptors for soul flame, soul beast, gui wu, and
 * other hun-dao visual/audio effects. Each descriptor includes FX ID, display name, and type
 * categorization.
 *
 * <p>Phase 5: Data-driven FX dispatch system.
 */
public final class HunDaoFxDescriptors {

  private HunDaoFxDescriptors() {}

  // ===== Soul Flame (魂焰) FX =====

  /** Soul flame DoT tick effect (particles + sound per tick). */
  public static final ResourceLocation SOUL_FLAME_TICK = ChestCavity.id("soulbeast_dot_tick");

  /** Soul flame ignition burst effect (initial application). */
  public static final ResourceLocation SOUL_FLAME_IGNITE = ChestCavity.id("soul_flame_ignite");

  /** Soul flame expiration effect (when DoT ends). */
  public static final ResourceLocation SOUL_FLAME_EXPIRE = ChestCavity.id("soul_flame_expire");

  // ===== Soul Beast (魂兽化) FX =====

  /** Soul beast activation transformation burst. */
  public static final ResourceLocation SOUL_BEAST_ACTIVATE = ChestCavity.id("soul_beast_activate");

  /** Soul beast ambient aura particles (while active). */
  public static final ResourceLocation SOUL_BEAST_AMBIENT = ChestCavity.id("soul_beast_ambient");

  /** Soul beast deactivation fade effect. */
  public static final ResourceLocation SOUL_BEAST_DEACTIVATE =
      ChestCavity.id("soul_beast_deactivate");

  /** Soul beast melee hit impact effect. */
  public static final ResourceLocation SOUL_BEAST_HIT = ChestCavity.id("soul_beast_hit");

  // ===== Gui Wu (鬼雾) FX =====

  /** Gui wu fog activation effect. */
  public static final ResourceLocation GUI_WU_ACTIVATE = ChestCavity.id("gui_wu_activate");

  /** Gui wu ambient fog particles (periodic). */
  public static final ResourceLocation GUI_WU_AMBIENT = ChestCavity.id("gui_wu_ambient");

  /** Gui wu fog dissipation effect (when duration expires). */
  public static final ResourceLocation GUI_WU_DISSIPATE = ChestCavity.id("gui_wu_dissipate");

  // ===== Hun Po (魂魄) Resource FX =====

  /** Hun po drain/leak particles (when soul beast leaks hun po). */
  public static final ResourceLocation HUN_PO_LEAK = ChestCavity.id("hun_po_leak");

  /** Hun po recovery burst (when gaining hun po). */
  public static final ResourceLocation HUN_PO_RECOVERY = ChestCavity.id("hun_po_recovery");

  /** Hun po critical low warning effect. */
  public static final ResourceLocation HUN_PO_LOW_WARNING = ChestCavity.id("hun_po_low_warning");

  // ===== Utility =====

  /**
   * FX category enumeration for filtering and debugging.
   *
   * <p>Allows FX router to categorize effects and apply category-specific logic if needed.
   */
  public enum FxCategory {
    SOUL_FLAME,
    SOUL_BEAST,
    GUI_WU,
    HUN_PO,
    UTILITY
  }

  /**
   * Returns the category for a given FX ID.
   *
   * @param fxId the FX resource location
   * @return the FX category, or UTILITY if unknown
   */
  public static FxCategory getCategoryFor(ResourceLocation fxId) {
    if (fxId == null) {
      return FxCategory.UTILITY;
    }

    String path = fxId.getPath();

    if (path.startsWith("soul_flame")) {
      return FxCategory.SOUL_FLAME;
    } else if (path.startsWith("soul_beast")) {
      return FxCategory.SOUL_BEAST;
    } else if (path.startsWith("gui_wu")) {
      return FxCategory.GUI_WU;
    } else if (path.startsWith("hun_po")) {
      return FxCategory.HUN_PO;
    }

    return FxCategory.UTILITY;
  }
}
