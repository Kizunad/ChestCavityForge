package net.tigereye.chestcavity.guscript.runtime.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tigereye.chestcavity.config.CCConfig;
import org.junit.jupiter.api.Test;

class FlowTimeScaleCombineTest {

  @Test
  void multiplyStrategyMultipliesAndClamps() {
    double result =
        GuScriptExecutor.combineTimeScale(1.2D, 1.5D, CCConfig.TimeScaleCombineStrategy.MULTIPLY);
    assertEquals(1.8D, result, 1.0E-6);

    double clampedLow =
        GuScriptExecutor.combineTimeScale(0.01D, 0.2D, CCConfig.TimeScaleCombineStrategy.MULTIPLY);
    assertEquals(0.1D, clampedLow, 1.0E-6);

    double clampedHigh =
        GuScriptExecutor.combineTimeScale(150.0D, 2.0D, CCConfig.TimeScaleCombineStrategy.MULTIPLY);
    assertEquals(100.0D, clampedHigh, 1.0E-6);
  }

  @Test
  void maxStrategyChoosesLarger() {
    double result =
        GuScriptExecutor.combineTimeScale(1.2D, 1.5D, CCConfig.TimeScaleCombineStrategy.MAX);
    assertEquals(1.5D, result, 1.0E-6);
  }

  @Test
  void overrideStrategyPrefersSession() {
    double result =
        GuScriptExecutor.combineTimeScale(2.0D, 1.5D, CCConfig.TimeScaleCombineStrategy.OVERRIDE);
    assertEquals(1.5D, result, 1.0E-6);
  }
}
