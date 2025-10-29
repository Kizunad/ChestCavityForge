package net.tigereye.chestcavity.guscript.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;

/**
 * Executes the wrapped actions only when the current GuScript target passes the configured checks.
 * The intent is to guard resource costs and FX emissions behind a valid remote target.
 */
public final class ConditionalTargetAction implements Action {

  public static final String ID = "if.target";

  private final double maxRangeSq;
  private final boolean excludeSelf;
  private final boolean requireAlive;
  private final boolean requirePerformer;
  private final List<Action> actions;

  public ConditionalTargetAction(
      double range,
      boolean excludeSelf,
      boolean requireAlive,
      boolean requirePerformer,
      List<Action> actions) {
    double sanitizedRange = range;
    if (Double.isNaN(sanitizedRange) || sanitizedRange <= 0.0D) {
      sanitizedRange = Double.POSITIVE_INFINITY;
    }
    this.maxRangeSq =
        Double.isInfinite(sanitizedRange)
            ? Double.POSITIVE_INFINITY
            : sanitizedRange * sanitizedRange;
    this.excludeSelf = excludeSelf;
    this.requireAlive = requireAlive;
    this.requirePerformer = requirePerformer;
    this.actions = actions == null ? List.of() : List.copyOf(actions);
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public String description() {
    return "条件触发(目标)";
  }

  /**
   * Executes the action.
   */
  @Override
  public void execute(GuScriptContext context) {
    Player performer = context.performer();
    if (requirePerformer && performer == null) {
      return;
    }
    LivingEntity target = context.target();
    if (target == null) {
      return;
    }
    if (excludeSelf && performer != null && target == performer) {
      return;
    }
    if (requireAlive && !target.isAlive()) {
      return;
    }
    if (performer != null && Double.isFinite(maxRangeSq)) {
      double distanceSq = performer.distanceToSqr(target);
      if (distanceSq > maxRangeSq) {
        return;
      }
    }
    for (Action action : actions) {
      action.execute(context);
    }
  }

  /**
   * Gets the condition.
   */
  public Predicate<LivingEntity> getCondition() {
    return null;
  }

  public List<Action> actions() {
    return new ArrayList<>(actions);
  }
}
