package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulRarity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.IHunDaoPanelTab;

/**
 * Soul Overview Tab implementation.
 *
 * <p>Phase 7.3: Displays soul state, level, rarity, max hun po, and attributes with full i18n
 * support.
 */
@OnlyIn(Dist.CLIENT)
public class SoulOverviewTab implements IHunDaoPanelTab {

  @NonNull
  @Override
  public String getId() {
    return "soul_overview";
  }

  @NonNull
  @Override
  public String getTitle() {
    return I18n.get("gui.chestcavity.hun_dao_modern_panel.soul_overview");
  }

  /**
   * Phase 7.2: Create custom two-column layout for soul fields and attributes.
   */
  @Nullable
  @Override
  public View createContentView(@NonNull Context context) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;

    if (player == null) {
      return null; // Fall back to text rendering
    }

    HunDaoClientState state = HunDaoClientState.instance();
    var playerId = player.getUUID();

    var root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);

    // Check if soul system is active
    boolean isActive = state.isSoulSystemActive(playerId);

    // Phase 7.2: Inactive warning card
    if (!isActive) {
      var warningCard = createWarningCard(context);
      var warningParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      warningParams.bottomMargin = root.dp(12);
      root.addView(warningCard, warningParams);
    }

    // Phase 7.2: Soul fields section (two-column grid)
    var fieldsSection = createSoulFieldsSection(context, state, playerId);
    var fieldsParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    fieldsParams.bottomMargin = root.dp(16);
    root.addView(fieldsSection, fieldsParams);

    // Phase 7.2: Divider
    var divider = createDivider(context);
    var dividerParams =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, root.dp(1));
    dividerParams.bottomMargin = root.dp(12);
    root.addView(divider, dividerParams);

    // Phase 7.2: Attributes section
    var attributesSection = createAttributesSection(context, state, playerId);
    root.addView(
        attributesSection,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }

  @NonNull
  @Override
  public String getFormattedContent() {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;

    if (player == null) {
      return I18n.get("text.chestcavity.hun_dao.no_player_data");
    }

    HunDaoClientState state = HunDaoClientState.instance();
    var playerId = player.getUUID();

    StringBuilder sb = new StringBuilder();
    sb.append(I18n.get("gui.chestcavity.hun_dao_modern_panel.soul_overview"));
    sb.append("\n\n");

    // Check if soul system is active
    boolean isActive = state.isSoulSystemActive(playerId);
    if (!isActive) {
      sb.append(I18n.get("text.chestcavity.hun_dao.system_inactive"));
      sb.append("\n");
      sb.append("→ Display fallback placeholder\n");
      sb.append("→ No crash, no missing data\n\n");
    }

    // Soul State
    sb.append(I18n.get("text.chestcavity.hun_dao.soul_state"));
    sb.append(formatSoulState(state.getSoulState(playerId).orElse(null)));
    sb.append("\n");

    // Soul Level
    sb.append(I18n.get("text.chestcavity.hun_dao.soul_level"));
    sb.append(formatSoulLevel(state.getSoulLevel(playerId)));
    sb.append("\n");

    // Soul Rarity
    sb.append(I18n.get("text.chestcavity.hun_dao.soul_rarity"));
    sb.append(formatSoulRarity(state.getSoulRarity(playerId).orElse(null)));
    sb.append("\n");

    // Soul Max (Hun Po Max)
    sb.append(I18n.get("text.chestcavity.hun_dao.soul_max"));
    sb.append(formatSoulMax((int) state.getHunPoMax(playerId)));
    sb.append("\n\n");

    // Attributes
    sb.append(I18n.get("text.chestcavity.hun_dao.attributes"));
    sb.append("\n");
    Map<String, Object> attributes = state.getSoulAttributes(playerId);
    String placeholder = I18n.get("text.chestcavity.hun_dao.placeholder");
    if (attributes.isEmpty()) {
      sb.append("  - Attribute 1: ").append(placeholder).append("\n");
      sb.append("  - Attribute 2: ").append(placeholder).append("\n");
      sb.append("  - Attribute 3: ").append(placeholder).append("\n");
    } else {
      int count = 0;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        count++;
        sb.append("  - ");
        sb.append(entry.getKey());
        sb.append(": ");
        sb.append(entry.getValue());
        sb.append("\n");
      }
      // Fill in placeholders if less than 3 attributes
      while (count < 3) {
        count++;
        sb.append("  - Attribute ");
        sb.append(count);
        sb.append(": ");
        sb.append(placeholder);
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  private String formatSoulState(SoulState state) {
    if (state == null) {
      return I18n.get(SoulState.UNKNOWN.getTranslationKey());
    }
    return I18n.get(state.getTranslationKey());
  }

  private String formatSoulLevel(int level) {
    return level > 0 ? String.valueOf(level) : I18n.get("text.chestcavity.hun_dao.placeholder");
  }

  private String formatSoulRarity(SoulRarity rarity) {
    if (rarity == null) {
      return I18n.get(SoulRarity.UNIDENTIFIED.getTranslationKey());
    }
    return I18n.get(rarity.getTranslationKey());
  }

  private String formatSoulMax(int max) {
    return max > 0 ? String.valueOf(max) : I18n.get("text.chestcavity.hun_dao.placeholder");
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  // ==================== Phase 7.2: Layout Helper Methods ====================

  /**
   * Create a warning card for inactive soul system.
   */
  private View createWarningCard(Context context) {
    var card = new LinearLayout(context);
    card.setOrientation(LinearLayout.VERTICAL);
    int padding = card.dp(12);
    card.setPadding(padding, padding, padding, padding);

    // Card background
    var background = new ShapeDrawable();
    background.setCornerRadius(card.dp(6));
    background.setColor(0xDD2A1F1A); // Warm dark background
    background.setStroke(card.dp(1), 0xFFE2904A); // Orange warning border
    card.setBackground(background);

    // Warning title
    var title = new TextView(context);
    title.setText(I18n.get("text.chestcavity.hun_dao.system_inactive"));
    title.setTextSize(14);
    title.setTextColor(0xFFE2904A);
    title.setGravity(Gravity.CENTER_HORIZONTAL);
    card.addView(
        title,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Warning description
    var desc = new TextView(context);
    desc.setText(I18n.get("text.chestcavity.hun_dao.no_player_data"));
    desc.setTextSize(12);
    desc.setTextColor(0xFFDFDFDF);
    desc.setGravity(Gravity.CENTER_HORIZONTAL);
    var descParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    descParams.topMargin = card.dp(4);
    card.addView(desc, descParams);

    return card;
  }

  /**
   * Create soul fields section with two-column layout.
   */
  private View createSoulFieldsSection(
      Context context, HunDaoClientState state, java.util.UUID playerId) {
    var section = new LinearLayout(context);
    section.setOrientation(LinearLayout.VERTICAL);

    // Soul State row
    addFieldRow(
        context,
        section,
        I18n.get("text.chestcavity.hun_dao.soul_state").replace(": ", ""),
        formatSoulState(state.getSoulState(playerId).orElse(null)));

    // Soul Level row
    addFieldRow(
        context,
        section,
        I18n.get("text.chestcavity.hun_dao.soul_level").replace(": ", ""),
        formatSoulLevel(state.getSoulLevel(playerId)));

    // Soul Rarity row
    addFieldRow(
        context,
        section,
        I18n.get("text.chestcavity.hun_dao.soul_rarity").replace(": ", ""),
        formatSoulRarity(state.getSoulRarity(playerId).orElse(null)));

    // Soul Max row
    addFieldRow(
        context,
        section,
        I18n.get("text.chestcavity.hun_dao.soul_max").replace(": ", ""),
        formatSoulMax((int) state.getHunPoMax(playerId)));

    return section;
  }

  /**
   * Add a two-column field row (label | value).
   */
  private void addFieldRow(Context context, LinearLayout parent, String label, String value) {
    var row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);

    // Label (left column)
    var labelView = new TextView(context);
    labelView.setText(label);
    labelView.setTextSize(13);
    labelView.setTextColor(0xFFAAAAAA); // Dimmed label color
    var labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    row.addView(labelView, labelParams);

    // Value (right column)
    var valueView = new TextView(context);
    valueView.setText(value);
    valueView.setTextSize(13);
    valueView.setTextColor(0xFFFFFFFF); // Bright value color
    valueView.setGravity(Gravity.END);
    var valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    row.addView(valueView, valueParams);

    // Add row to parent
    var rowParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.bottomMargin = row.dp(6);
    parent.addView(row, rowParams);
  }

  /**
   * Create a horizontal divider line.
   */
  private View createDivider(Context context) {
    var divider = new View(context);
    var background = new ShapeDrawable();
    background.setColor(0xFF3A7BC8); // Match border color
    divider.setBackground(background);
    return divider;
  }

  /**
   * Create attributes section with grid layout.
   */
  private View createAttributesSection(
      Context context, HunDaoClientState state, java.util.UUID playerId) {
    var section = new LinearLayout(context);
    section.setOrientation(LinearLayout.VERTICAL);

    // Section title
    var title = new TextView(context);
    title.setText(I18n.get("text.chestcavity.hun_dao.attributes").replace(":", ""));
    title.setTextSize(14);
    title.setTextColor(0xFFFFFFFF);
    var titleParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    titleParams.bottomMargin = section.dp(8);
    section.addView(title, titleParams);

    // Attributes list
    Map<String, Object> attributes = state.getSoulAttributes(playerId);
    String placeholder = I18n.get("text.chestcavity.hun_dao.placeholder");
    if (attributes.isEmpty()) {
      // Show placeholders
      addAttributeItem(context, section, "Attribute 1", placeholder);
      addAttributeItem(context, section, "Attribute 2", placeholder);
      addAttributeItem(context, section, "Attribute 3", placeholder);
    } else {
      int count = 0;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        count++;
        addAttributeItem(context, section, entry.getKey(), String.valueOf(entry.getValue()));
      }
      // Fill placeholders if less than 3
      while (count < 3) {
        count++;
        addAttributeItem(context, section, "Attribute " + count, placeholder);
      }
    }

    return section;
  }

  /**
   * Add an attribute item to the attributes section.
   */
  private void addAttributeItem(Context context, LinearLayout parent, String name, String value) {
    var item = new LinearLayout(context);
    item.setOrientation(LinearLayout.HORIZONTAL);

    // Bullet/prefix
    var bullet = new TextView(context);
    bullet.setText("• ");
    bullet.setTextSize(12);
    bullet.setTextColor(0xFF4A90E2);
    item.addView(
        bullet,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Attribute name
    var nameView = new TextView(context);
    nameView.setText(name + ": ");
    nameView.setTextSize(12);
    nameView.setTextColor(0xFFDFDFDF);
    item.addView(
        nameView,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Attribute value
    var valueView = new TextView(context);
    valueView.setText(value);
    valueView.setTextSize(12);
    valueView.setTextColor(0xFFFFFFFF);
    item.addView(
        valueView,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Add item to parent
    var itemParams =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    itemParams.bottomMargin = item.dp(4);
    parent.addView(item, itemParams);
  }
}
