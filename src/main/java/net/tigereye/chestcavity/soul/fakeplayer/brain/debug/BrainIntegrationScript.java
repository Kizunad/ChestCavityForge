package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.arbitration.Arbitrator;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.BudgetPolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.HysteresisPolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.policy.ResidencePolicy;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ScoreInputs;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.WeightedUtilityScorer;

/**
 * 集成调试脚本：将评分器、策略、仲裁器组合在一起， 为后续串行接线提供可复用的离线决策逻辑。
 *
 * <p>该脚本并不会直接修改当前运行时，只提供一个可配置的 决策器，用于计算“应当切换到哪个子大脑”以及“并行动作”建议。
 */
public final class BrainIntegrationScript {

  public record ActionCandidate(String id, double score) {}

  public record Decision(
      String primaryBrainId,
      BrainMode fallbackMode,
      double combatScore,
      double survivalScore,
      double explorationScore,
      double idleScore,
      Arbitrator.Result<ActionCandidate> arbitration,
      boolean allowStateChange,
      boolean allowPlanning) {}

  private final WeightedUtilityScorer combatScorer;
  private final WeightedUtilityScorer survivalScorer;
  private final WeightedUtilityScorer explorationScorer;
  private final HysteresisPolicy hysteresis;
  private final ResidencePolicy residence;
  private final BudgetPolicy budget;
  private final Arbitrator<ActionCandidate> arbitrator;

  private BrainIntegrationScript(Builder builder) {
    this.combatScorer = builder.combatScorer;
    this.survivalScorer = builder.survivalScorer;
    this.explorationScorer = builder.explorationScorer;
    this.hysteresis = builder.hysteresis;
    this.residence = builder.residence;
    this.budget = builder.budget;
    this.arbitrator = builder.arbitrator;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * 根据当前评分输入、活跃子大脑与预算限制，给出下一步建议。
   *
   * @param inputs 每 tick 聚合的评分输入
   * @param currentBrainId 当前激活的子大脑 ID（如 "combat"、"survival"、"exploration"、"idle"）
   * @param elapsedTicks 当前模式驻留时间
   * @param stateChangesThisTick 本 tick 已执行的状态切换次数
   * @param plansThisTick 本 tick 已规划的动作数量
   */
  public Decision evaluate(
      ScoreInputs inputs,
      String currentBrainId,
      int elapsedTicks,
      int stateChangesThisTick,
      int plansThisTick) {
    Objects.requireNonNull(inputs, "inputs");
    String normalizedId = normalizeBrainId(currentBrainId);

    boolean allowStateChange = stateChangesThisTick < budget.maxStateChangesPerTick();
    boolean allowPlanning = plansThisTick < budget.maxPlansPerTick();

    double threatLevel = inputs.primaryTarget() != null ? 1.0 : 0.0;
    double distanceScore =
        normalizeDistance(
            inputs.primaryTarget() != null ? inputs.distanceToTarget() : Double.POSITIVE_INFINITY);
    double healthRatio = inputs.healthRatio();
    boolean hasRegen = inputs.hasRegen();
    boolean inDanger = inputs.inDanger();

    double combatScore =
        combatScorer.score(threatLevel, distanceScore, healthRatio, hasRegen, inDanger);
    double survivalScore =
        survivalScorer.score(
            Math.max(threatLevel, inDanger ? 1.0 : 0.0),
            distanceScore,
            healthRatio,
            hasRegen,
            inDanger);
    double explorationScore =
        explorationScorer.score(
            1.0 - threatLevel, 1.0 - distanceScore, healthRatio, hasRegen, !inDanger);
    double idleScore =
        Math.max(0.0, 1.0 - Math.max(Math.max(combatScore, survivalScore), explorationScore));

    ModeCandidate current =
        pickCurrent(normalizedId, combatScore, survivalScore, explorationScore, idleScore);
    ModeCandidate target =
        pickTarget(
            current,
            elapsedTicks,
            combatScore,
            survivalScore,
            explorationScore,
            idleScore,
            allowStateChange);
    if (!allowStateChange || !residence.canLeave(current.mode(), elapsedTicks)) {
      target = current;
    }
    if (target != current && !hysteresis.shouldSwitch(current.score(), target.score())) {
      target = current;
    }

    Arbitrator.Result<ActionCandidate> arbitration =
        allowPlanning
            ? arbitrator.decide(
                defaultActionCandidates(combatScore, survivalScore, explorationScore),
                ActionCandidate::score)
            : new Arbitrator.Result<>(null, List.of());

    return new Decision(
        target.id(),
        target.mode(),
        combatScore,
        survivalScore,
        explorationScore,
        idleScore,
        arbitration,
        allowStateChange,
        allowPlanning);
  }

  private static String normalizeBrainId(String id) {
    if (id == null || id.isBlank()) {
      return "idle";
    }
    return id.trim().toLowerCase(Locale.ROOT);
  }

  private static double normalizeDistance(double distance) {
    if (Double.isInfinite(distance) || Double.isNaN(distance)) {
      return 0.0;
    }
    // 0-16 格内视作高威胁（接近 1），远距衰减
    double clamped = Math.max(0.0, Math.min(1.0, 1.0 - (distance / 16.0)));
    return clamped;
  }

  private static ModeCandidate pickCurrent(
      String currentId,
      double combatScore,
      double survivalScore,
      double explorationScore,
      double idleScore) {
    return switch (currentId) {
      case "combat" -> new ModeCandidate("combat", BrainMode.COMBAT, combatScore);
      case "survival" -> new ModeCandidate("survival", BrainMode.SURVIVAL, survivalScore);
      case "exploration" -> new ModeCandidate("exploration", BrainMode.IDLE, explorationScore);
      case "idle", "auto" -> new ModeCandidate("idle", BrainMode.IDLE, idleScore);
      default -> new ModeCandidate(currentId, BrainMode.IDLE, idleScore);
    };
  }

  private ModeCandidate pickTarget(
      ModeCandidate current,
      int elapsedTicks,
      double combatScore,
      double survivalScore,
      double explorationScore,
      double idleScore,
      boolean allowStateChange) {
    if (!allowStateChange) {
      return current;
    }
    List<ModeCandidate> candidates =
        List.of(
            new ModeCandidate("combat", BrainMode.COMBAT, combatScore),
            new ModeCandidate("survival", BrainMode.SURVIVAL, survivalScore),
            new ModeCandidate("exploration", BrainMode.IDLE, explorationScore),
            new ModeCandidate("idle", BrainMode.IDLE, idleScore));
    ModeCandidate best = current;
    for (ModeCandidate candidate : candidates) {
      if (candidate.score() <= best.score()) {
        continue;
      }
      if (!residence.canLeave(current.mode(), elapsedTicks) && candidate.mode() != current.mode()) {
        continue;
      }
      best = candidate;
    }
    return best;
  }

  private static List<ActionCandidate> defaultActionCandidates(
      double combatScore, double survivalScore, double explorationScore) {
    List<ActionCandidate> list = new ArrayList<>(3);
    list.add(new ActionCandidate("action/force_fight", combatScore));
    list.add(new ActionCandidate("action/heal", survivalScore));
    list.add(new ActionCandidate("action/explore_path", explorationScore));
    return list;
  }

  public static final class Builder {
    private WeightedUtilityScorer combatScorer;
    private WeightedUtilityScorer survivalScorer;
    private WeightedUtilityScorer explorationScorer;
    private HysteresisPolicy hysteresis;
    private ResidencePolicy residence;
    private BudgetPolicy budget;
    private Arbitrator<ActionCandidate> arbitrator;

    private Builder() {
      this.combatScorer = new WeightedUtilityScorer(0.45, 0.30, 0.15, 0.05, 0.05);
      this.survivalScorer = new WeightedUtilityScorer(0.25, 0.10, 0.40, 0.15, 0.10);
      this.explorationScorer = new WeightedUtilityScorer(0.30, 0.25, 0.10, 0.10, 0.25);
      this.hysteresis = new HysteresisPolicy(0.05);
      this.residence = new ResidencePolicy(40);
      this.budget = new BudgetPolicy(1, 2);
      this.arbitrator = new Arbitrator<>(2, 0.35);
    }

    public Builder combatScorer(WeightedUtilityScorer scorer) {
      this.combatScorer = Objects.requireNonNull(scorer, "combatScorer");
      return this;
    }

    public Builder survivalScorer(WeightedUtilityScorer scorer) {
      this.survivalScorer = Objects.requireNonNull(scorer, "survivalScorer");
      return this;
    }

    public Builder explorationScorer(WeightedUtilityScorer scorer) {
      this.explorationScorer = Objects.requireNonNull(scorer, "explorationScorer");
      return this;
    }

    public Builder hysteresis(HysteresisPolicy hysteresis) {
      this.hysteresis = Objects.requireNonNull(hysteresis, "hysteresis");
      return this;
    }

    public Builder residence(ResidencePolicy residence) {
      this.residence = Objects.requireNonNull(residence, "residence");
      return this;
    }

    public Builder budget(BudgetPolicy budget) {
      this.budget = Objects.requireNonNull(budget, "budget");
      return this;
    }

    public Builder arbitrator(Arbitrator<ActionCandidate> arbitrator) {
      this.arbitrator = Objects.requireNonNull(arbitrator, "arbitrator");
      return this;
    }

    public BrainIntegrationScript build() {
      return new BrainIntegrationScript(this);
    }
  }

  private record ModeCandidate(String id, BrainMode mode, double score) {}
}
