package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;

/** 指挥棒可选择的战术集合。每个战术绑定一个默认轨迹类型，并提供本地化键。 */
public enum CommandTactic {
  FOCUS_FIRE(
      "focus_fire",
      "text.guzhenren.jianyingu.tactic.focus_fire",
      "text.guzhenren.jianyingu.tactic.focus_fire.desc",
      TrajectoryType.PredictiveLine),
  INTERCEPT(
      "intercept",
      "text.guzhenren.jianyingu.tactic.intercept",
      "text.guzhenren.jianyingu.tactic.intercept.desc",
      TrajectoryType.CurvedIntercept),
  SUPPRESS(
      "suppress",
      "text.guzhenren.jianyingu.tactic.suppress",
      "text.guzhenren.jianyingu.tactic.suppress.desc",
      TrajectoryType.Serpentine),
  SHEPHERD(
      "shepherd",
      "text.guzhenren.jianyingu.tactic.shepherd",
      "text.guzhenren.jianyingu.tactic.shepherd.desc",
      TrajectoryType.VortexOrbit),
  DUEL(
      "duel",
      "text.guzhenren.jianyingu.tactic.duel",
      "text.guzhenren.jianyingu.tactic.duel.desc",
      TrajectoryType.Corkscrew);

  private final String id;
  private final String nameKey;
  private final String descriptionKey;
  private final TrajectoryType trajectoryType;

  CommandTactic(String id, String nameKey, String descriptionKey, TrajectoryType trajectoryType) {
    this.id = id;
    this.nameKey = nameKey;
    this.descriptionKey = descriptionKey;
    this.trajectoryType = trajectoryType;
  }

  public String id() {
    return id;
  }

  public Component displayName() {
    return Component.translatable(nameKey);
  }

  public Component description() {
    return Component.translatable(descriptionKey);
  }

  public TrajectoryType trajectory() {
    return trajectoryType;
  }

  public static Optional<CommandTactic> byId(String id) {
    if (id == null || id.isEmpty()) {
      return Optional.empty();
    }
    String normalized = id.toLowerCase(Locale.ROOT);
    for (CommandTactic tactic : values()) {
      if (tactic.id.equals(normalized)) {
        return Optional.of(tactic);
      }
    }
    return Optional.empty();
  }
}
