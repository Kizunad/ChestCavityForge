package net.tigereye.chestcavity.soul.fakeplayer.brain.arbitration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Generic arbitrator to select one exclusive choice and several concurrent choices subject to a
 * score threshold and a hard cap.
 */
public final class Arbitrator<T> {

  public record Result<T>(T exclusive, List<T> concurrent) {}

  private final int maxConcurrent;
  private final double concurrentThreshold;

  public Arbitrator(int maxConcurrent, double concurrentThreshold) {
    this.maxConcurrent = Math.max(0, maxConcurrent);
    this.concurrentThreshold = Math.max(0.0, Math.min(1.0, concurrentThreshold));
  }

  /**
   * @param candidates items to consider
   * @param scorer returns a score in [0,1]
   */
  public Result<T> decide(List<T> candidates, java.util.function.ToDoubleFunction<T> scorer) {
    Objects.requireNonNull(candidates, "candidates");
    List<T> list = new ArrayList<>(candidates);
    list.sort(Comparator.comparingDouble((T t) -> scorer.applyAsDouble(t)).reversed());
    T exclusive = list.isEmpty() ? null : list.get(0);
    List<T> concurrent = new ArrayList<>();
    for (int i = 1; i < list.size() && concurrent.size() < maxConcurrent; i++) {
      T t = list.get(i);
      if (scorer.applyAsDouble(t) >= concurrentThreshold) {
        concurrent.add(t);
      } else {
        break;
      }
    }
    return new Result<>(exclusive, List.copyOf(concurrent));
  }
}
