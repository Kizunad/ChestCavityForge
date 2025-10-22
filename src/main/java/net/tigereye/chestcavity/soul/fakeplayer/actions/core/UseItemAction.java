package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;

/** Generic single-shot item use action. Parses item/hand (and potion variant) from id. */
public final class UseItemAction implements Action {
  private final ActionId id;
  private final ResourceLocation itemId;
  private final String extra; // optional variant, e.g., "healing" for potions
  private final InteractionHand hand;

  public UseItemAction(
      ResourceLocation fullId, ResourceLocation itemId, String extra, InteractionHand hand) {
    this.id = new ActionId(fullId);
    this.itemId = itemId;
    this.extra = extra;
    this.hand = hand == null ? InteractionHand.OFF_HAND : hand;
  }

  @Override
  public ActionId id() {
    return id;
  }

  @Override
  public ActionPriority priority() {
    return ActionPriority.NORMAL;
  }

  @Override
  public boolean allowConcurrent() {
    return true;
  }

  @Override
  public boolean canRun(ActionContext ctx) {
    return ctx.soul().isAlive();
  }

  @Override
  public void start(ActionContext ctx) {
    /* no-op */
  }

  @Override
  public ActionResult tick(ActionContext ctx) {
    var soul = ctx.soul();
    if (!soul.isAlive()) return ActionResult.FAILED;

    ItemStack stack = buildStack();
    if (stack.isEmpty()) return ActionResult.FAILED;

    ItemStack prev = soul.getItemInHand(hand).copy();
    float before = soul.getHealth();
    soul.setItemInHand(hand, stack);
    boolean used =
        net.tigereye.chestcavity.soul.util.SoulPlayerInput.rightMouseItemUse(soul, hand, true);
    ItemStack remain = soul.getItemInHand(hand);
    soul.setItemInHand(hand, ItemStack.EMPTY);
    if (!remain.isEmpty()) {
      var inv = soul.getInventory();
      if (!inv.add(remain.copy())) soul.drop(remain.copy(), false);
    }
    soul.setItemInHand(hand, prev);
    float after = soul.getHealth();
    ChestCavity.LOGGER.info(
        "[soul][action][use_item] id={} item={} hand={} used={} hp:{}->{}",
        id,
        itemId,
        hand,
        used,
        before,
        after);
    return ActionResult.SUCCESS;
  }

  @Override
  public void cancel(ActionContext ctx) {
    /* single-shot */
  }

  @Override
  public String cooldownKey() {
    return null;
  }

  @Override
  public long nextReadyAt(ActionContext ctx, long now) {
    return now;
  }

  private ItemStack buildStack() {
    Item item = BuiltInRegistries.ITEM.get(itemId);
    if (item == null || item == Items.AIR) return ItemStack.EMPTY;
    if (item == Items.POTION) {
      if ("healing".equalsIgnoreCase(extra)) {
        return PotionContents.createItemStack(Items.POTION, Potions.HEALING);
      }
      // default to plain water if unknown variant
      return new ItemStack(Items.POTION);
    }
    return new ItemStack(item);
  }
}
