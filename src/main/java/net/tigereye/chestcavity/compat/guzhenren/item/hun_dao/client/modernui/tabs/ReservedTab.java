package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs;

import icyllis.modernui.annotation.NonNull;

import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.IHunDaoPanelTab;

/**
 * Reserved Tab placeholder implementation.
 *
 * <p>Phase 7.3: Placeholder for future expansion (Phase 8+) with i18n support.
 */
@OnlyIn(Dist.CLIENT)
public class ReservedTab implements IHunDaoPanelTab {

  private final String id;
  private final String title;

  /**
   * Create a reserved tab with a custom ID and title.
   *
   * @param id the tab ID
   * @param title the tab title
   */
  public ReservedTab(String id, String title) {
    this.id = id;
    this.title = title;
  }

  @NonNull
  @Override
  public String getId() {
    return id;
  }

  @NonNull
  @Override
  public String getTitle() {
    return title;
  }

  @NonNull
  @Override
  public String getFormattedContent() {
    return I18n.get("text.chestcavity.hun_dao.coming_soon")
        + "\n\n"
        + I18n.get("text.chestcavity.hun_dao.reserved_future")
        + "\n\n"
        + I18n.get("text.chestcavity.hun_dao.future_phase");
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    // Reserved tabs are visible but not enabled (non-clickable)
    return false;
  }
}
