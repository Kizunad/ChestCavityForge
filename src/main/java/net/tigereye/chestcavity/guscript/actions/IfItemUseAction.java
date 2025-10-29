package net.tigereye.chestcavity.guscript.actions;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * If-guard that attempts to right-click use a specific item (by id) and, if successful, applies an
 * ender-pearl-like cooldown to that item for the performer, then runs nested actions.
 *
 * <p>Parameters: - itemId: namespaced id, e.g. "guzhenren:ling_xian_gu"（灵涎蛊） - cooldownTicks:
 * cooldown ticks to apply after successful use (default 40 = 2s) - preferOffhand: if true and
 * offhand already holds the item, use OFF_HAND first - actions: nested actions to execute only when
 * use succeeds
 */
public record IfItemUseAction(
    ResourceLocation itemId, int cooldownTicks, boolean preferOffhand, List<Action> actions)
    implements Action {

  public static final String ID = "if.item_use";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "条件: 使用物品 -> " + itemId + " 冷却=" + cooldownTicks + "t";
  }

  @Override
  public void execute(GuScriptContext context) {
    Player performer = context.performer();
    if (performer == null || performer.level().isClientSide()) return;
    if (itemId == null || actions == null || actions.isEmpty()) return;

    Item item = BuiltInRegistries.ITEM.get(itemId);
    if (item == null) return;
    if (performer.getCooldowns().isOnCooldown(item)) return;

    // 1) Choose hand/slot
    Pair<InteractionHand, Integer> choice = chooseHandAndSlot(performer, item, preferOffhand);
    if (choice == null) return;

    InteractionHand hand = choice.getFirst();
    Integer slot = choice.getSecond();
    int originalSelected = performer.getInventory().selected;
    boolean switched = false;
    if (slot != null && slot >= 0 && slot <= 8 && performer.getInventory().selected != slot) {
      performer.getInventory().selected = slot;
      switched = true;
    }

    // 2) Attempt use
    // Resolve the actual stack from the player's hand; never fabricate a new stack
    ItemStack stack = performer.getItemInHand(hand);
    if (stack.isEmpty() || stack.getItem() != item) {
      // Selection may have changed; try to re-resolve once from the target hand
      stack = performer.getItemInHand(hand);
      if (stack.isEmpty() || stack.getItem() != item) {
        if (switched) performer.getInventory().selected = originalSelected;
        return;
      }
    }
    InteractionResultHolder<ItemStack> result = stack.use(performer.level(), performer, hand);
    InteractionResult type = result.getResult();
    boolean consumed = type.consumesAction() || type == InteractionResult.SUCCESS;
    if (!consumed) {
      // Revert selection if we changed it and failed
      if (switched) performer.getInventory().selected = originalSelected;
      return;
    }

    ItemStack after = result.getObject();
    // Only write back when the returned reference differs; this preserves in-place mutations
    if (after != stack) {
      performer.setItemInHand(hand, after);
    }
    if (after.getUseDuration(performer) > 0 && !performer.isUsingItem()) {
      performer.startUsingItem(hand);
    }

    // 3) Apply cooldown like ender pearl
    int cd = Math.max(0, cooldownTicks);
    if (cd > 0) performer.getCooldowns().addCooldown(item, cd);

    // 4) Run nested actions after a successful use
    for (Action a : actions) a.execute(context);
  }

  private static Pair<InteractionHand, Integer> chooseHandAndSlot(
      Player p, Item item, boolean preferOffhand) {
    // If offhand already holds the item and preferred, use it
    if (preferOffhand && p.getOffhandItem().getItem() == item) {
      return Pair.of(InteractionHand.OFF_HAND, null);
    }
    // Main hand
    if (p.getMainHandItem().getItem() == item) {
      return Pair.of(InteractionHand.MAIN_HAND, p.getInventory().selected);
    }
    // Search hotbar
    for (int i = 0; i < 9; i++) {
      ItemStack s = p.getInventory().getItem(i);
      if (!s.isEmpty() && s.getItem() == item) {
        return Pair.of(InteractionHand.MAIN_HAND, i);
      }
    }
    // If offhand holds it (even if not preferred), use offhand
    if (p.getOffhandItem().getItem() == item) {
      return Pair.of(InteractionHand.OFF_HAND, null);
    }
    return null;
  }
}
