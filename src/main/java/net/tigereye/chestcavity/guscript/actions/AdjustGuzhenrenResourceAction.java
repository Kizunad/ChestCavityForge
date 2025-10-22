package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Adjusts a Guzhenren resource (e.g., zhenyuan/jingli) by a signed amount. Positive increases,
 * negative consumes. No-op for non-players or missing attachments.
 */
public record AdjustGuzhenrenResourceAction(String identifier, double amount) implements Action {

  public static final String ID = "guzhenren.adjust";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "调整资源 " + identifier + " (" + amount + ")";
  }

  @Override
  public void execute(GuScriptContext context) {
    Player performer = context.performer();
    if (performer == null || identifier == null || identifier.isBlank()) {
      return;
    }
    GuzhenrenResourceBridge.open(performer)
        .ifPresent(handle -> handle.adjustDouble(identifier, amount, true));
  }
}
