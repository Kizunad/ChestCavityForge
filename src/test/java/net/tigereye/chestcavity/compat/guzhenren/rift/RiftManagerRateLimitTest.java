package net.tigereye.chestcavity.compat.guzhenren.rift;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 纯逻辑单元测试：验证裂隙伤害限频门的基础行为。
 *
 * <p>注意：不依赖 Minecraft 核心类，直接调用 UUID 版本的限频方法。
 */
public class RiftManagerRateLimitTest {

  @Test
  void firstPass_thenBlockWithinWindow_thenPassAfterWindow() {
    var manager = RiftManager.getInstance();
    UUID target = UUID.randomUUID();

    long t0 = 10_000L;
    int window = RiftTuning.RATE_LIMIT_WINDOW_TICKS;

    // 首次通过
    assertTrue(manager.tryPassDamageGate(target, t0));

    // 窗口内拒绝
    assertFalse(manager.tryPassDamageGate(target, t0 + 1));
    assertFalse(manager.tryPassDamageGate(target, t0 + window - 1));

    // 窗口边界通过
    assertTrue(manager.tryPassDamageGate(target, t0 + window));
  }

  @Test
  void multipleTargets_doNotInterfere() {
    var manager = RiftManager.getInstance();
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();

    long t0 = 20_000L;

    // A 首次通过
    assertTrue(manager.tryPassDamageGate(a, t0));
    // B 不受 A 影响，首次也应通过
    assertTrue(manager.tryPassDamageGate(b, t0 + 1));

    // A 窗口内再次尝试，拒绝
    assertFalse(manager.tryPassDamageGate(a, t0 + 2));
    // B 窗口内再次尝试，拒绝
    assertFalse(manager.tryPassDamageGate(b, t0 + 2));

    // 窗口之后再次尝试，均应通过
    int window = RiftTuning.RATE_LIMIT_WINDOW_TICKS;
    assertTrue(manager.tryPassDamageGate(a, t0 + window));
    assertTrue(manager.tryPassDamageGate(b, t0 + 1 + window));
  }
}
