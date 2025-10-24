package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.gufang.GuFangRecipe;
import net.tigereye.chestcavity.compat.guzhenren.gufang.GuFangRecipeRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;

public final class RefineGuAction implements Action {

  private static final ActionId ID =
      new ActionId(ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/refine_gu"));
  private static final int INTERVAL_TICKS = 20;
  private static final boolean DEBUG = Boolean.getBoolean("chestcavity.debugGuFang");

  private final ActionId configuredId;
  private final net.minecraft.resources.ResourceLocation explicitGuFang; // optional override

  public RefineGuAction() {
    this.configuredId = ID;
    this.explicitGuFang = null;
  }

  public RefineGuAction(ResourceLocation actionId, ResourceLocation guFangId) {
    this.configuredId = new ActionId(actionId);
    this.explicitGuFang = guFangId;
  }

  @Override
  public ActionId id() {
    return configuredId;
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
    return ctx.owner() != null && ctx.soul().isAlive();
  }

  @Override
  public void start(ActionContext ctx) {}

  @Override
  public ActionResult tick(ActionContext ctx) {
    ServerPlayer owner = ctx.owner();
    SoulPlayer soul = ctx.soul();
    if (owner == null) {
      debug("tick: owner=null, abort");
      return ActionResult.FAILED;
    }

    String guFangId;
    if (explicitGuFang != null) {
      guFangId = explicitGuFang.toString();
    } else {
      Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
          GuzhenrenResourceBridge.open(owner);
      if (handleOpt.isEmpty()) {
        debug("tick: open(owner) failed, abort");
        return ActionResult.FAILED;
      }
      guFangId = handleOpt.get().readString("GuFang").orElse("");
    }
    debug(
        "tick: soul={} guFangId={} source={}",
        soul.getSoulId(),
        guFangId,
        (explicitGuFang != null ? "explicit" : "owner-attachment"));
    if (guFangId.isBlank()) {
      debug("tick: blank guFangId, abort");
      return ActionResult.FAILED;
    }
    Optional<GuFangRecipe> recipeOpt = GuFangRecipeRegistry.findByGuFangId(guFangId);
    if (recipeOpt.isEmpty()) {
      debug("tick: recipe not found: {}", guFangId);
      return ActionResult.FAILED;
    }
    GuFangRecipe recipe = recipeOpt.get();
    if (DEBUG) {
      StringBuilder sb = new StringBuilder();
      sb.append("tick: recipe=")
          .append(recipe.id)
          .append(" base_success=")
          .append(recipe.baseSuccess)
          .append(" inputs=[");
      for (int i = 0; i < recipe.inputs.size(); i++) {
        var ing = recipe.inputs.get(i);
        sb.append(ing.item()).append(" x").append(ing.count());
        if (i + 1 < recipe.inputs.size()) sb.append(", ");
      }
      sb.append("] output=").append(recipe.output.item).append(" x").append(recipe.output.count);
      debug(sb.toString());
    }

    if (!hasAllInputs(soul, recipe)) {
      debug("tick: missing inputs, abort");
      return ActionResult.FAILED;
    }
    if (!consumeInputs(soul, recipe)) {
      debug("tick: consumeInputs failed unexpectedly");
      return ActionResult.FAILED;
    }
    debug("tick: inputs consumed for {}", guFangId);

    double p = recipe.baseSuccess;
    net.minecraft.util.RandomSource rng = soul.getRandom();
    double roll = rng.nextDouble();
    boolean success = roll < p;
    if (success) {
      ItemStack out = recipe.output.createStack();
      if (!out.isEmpty()) soul.getInventory().placeItemBackInInventory(out);
    }
    debug(
        "attempt: soul={} gufang={} roll={} p={} success={} out={}x{}",
        soul.getSoulId(),
        guFangId,
        String.format("%.4f", roll),
        String.format("%.4f", p),
        success,
        recipe.output.item,
        recipe.output.count);
    return ActionResult.RUNNING;
  }

  @Override
  public void cancel(ActionContext ctx) {}

  @Override
  public String cooldownKey() {
    return null;
  }

  @Override
  public long nextReadyAt(ActionContext ctx, long now) {
    return now + INTERVAL_TICKS;
  }

  private static boolean hasAllInputs(SoulPlayer soul, GuFangRecipe r) {
    boolean ok = true;
    for (GuFangRecipe.IngredientSpec ing : r.inputs) {
      int need = ing.count();
      int have = countInInventory(soul, ing);
      debug("check: need {} x{} have {}", ing.item(), need, have);
      if (have < need) ok = false;
    }
    return ok;
  }

  private static int countInInventory(SoulPlayer soul, GuFangRecipe.IngredientSpec ing) {
    int total = 0;
    total += countStack(ing, soul.getMainHandItem());
    total += countStack(ing, soul.getOffhandItem());
    for (ItemStack stack : soul.getInventory().items) {
      total += countStack(ing, stack);
      if (total >= ing.count()) break;
    }
    return total;
  }

  private static int countStack(GuFangRecipe.IngredientSpec ing, ItemStack stack) {
    if (stack == null || stack.isEmpty()) return 0;
    return ing.matches(stack) ? stack.getCount() : 0;
  }

  private static boolean consumeInputs(SoulPlayer soul, GuFangRecipe r) {
    for (GuFangRecipe.IngredientSpec ing : r.inputs) {
      int remaining = ing.count();
      debug("consume: target {} x{}", ing.item(), ing.count());
      remaining = tryDrainFromStack(soul, ing, remaining, true);
      remaining = tryDrainFromStack(soul, ing, remaining, false);
      if (remaining > 0) remaining = tryDrainFromInventory(soul, ing, remaining);
      if (remaining > 0) {
        debug("consume: fail for {} remaining {}", ing.item(), remaining);
        return false;
      }
    }
    return true;
  }

  private static int tryDrainFromStack(
      SoulPlayer soul, GuFangRecipe.IngredientSpec ing, int remaining, boolean offhand) {
    if (remaining <= 0) return 0;
    ItemStack stack = offhand ? soul.getOffhandItem() : soul.getMainHandItem();
    if (!ing.matches(stack)) return remaining;
    int take = Math.min(remaining, stack.getCount());
    stack.shrink(take);
    debug(
        "consume: {} hand take {} of {} (stack now {} )",
        offhand ? "off" : "main",
        take,
        ing.item(),
        stack.getCount());
    return remaining - take;
  }

  private static int tryDrainFromInventory(
      SoulPlayer soul, GuFangRecipe.IngredientSpec ing, int remaining) {
    var inv = soul.getInventory();
    for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
      ItemStack stack = inv.items.get(i);
      if (!ing.matches(stack)) continue;
      int take = Math.min(remaining, stack.getCount());
      stack.shrink(take);
      remaining -= take;
      debug(
          "consume: slot {} take {} of {} (stack now {} )", i, take, ing.item(), stack.getCount());
    }
    return remaining;
  }

  private static void debug(String fmt, Object... args) {
    if (!DEBUG) return;
    try {
      ChestCavity.LOGGER.info("[GuFang] " + fmt, args);
    } catch (Throwable ignored) {
    }
  }
}
