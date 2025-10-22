package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

/** 子脑读取的“冻结版意图”，在本 tick 内不变化。 目前仅承载一个主动意图；未来可扩展为意图栈与优先级。 */
public record IntentSnapshot(BrainIntent intent, int remainingTicks) {
  public static IntentSnapshot empty() {
    return new IntentSnapshot(null, 0);
  }

  public boolean isPresent() {
    return intent != null && remainingTicks > 0;
  }
}
