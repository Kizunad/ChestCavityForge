package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.Action;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionFactory;

/** Parses ids like chestcavity:action/use_item/<ns>/<path>[/<variant>[/<hand>]] */
public final class UseItemActionFactory implements ActionFactory {
  private static final String NS = "chestcavity";
  private static final String PREFIX = "action/use_item/";

  @Override
  public boolean supports(net.minecraft.resources.ResourceLocation id) {
    return NS.equals(id.getNamespace()) && id.getPath().startsWith(PREFIX);
  }

  @Override
  public Action create(ResourceLocation id) {
    String tail = id.getPath().substring(PREFIX.length());
    // Expect segments: ns/path[/variant[/hand]]
    String[] seg = tail.split("/");
    if (seg.length < 2) return null;
    String itemNs = seg[0];
    String itemPath = seg[1];
    String variant = seg.length >= 3 ? seg[2] : null;
    InteractionHand hand = InteractionHand.OFF_HAND;
    if (seg.length >= 4) {
      String h = seg[3].toLowerCase(java.util.Locale.ROOT);
      if (h.startsWith("main")) hand = InteractionHand.MAIN_HAND;
      else hand = InteractionHand.OFF_HAND;
    }
    ResourceLocation itemId;
    try {
      itemId = ResourceLocation.fromNamespaceAndPath(itemNs, itemPath);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return new UseItemAction(id, itemId, variant, hand);
  }
}
