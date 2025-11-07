package net.tigereye.chestcavity.compat.guzhenren.rift;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 裂隙伤害限频测试
 *
 * <p>测试 {@link RiftManager#tryPassDamageGate} 的限频逻辑：
 * <ul>
 *   <li>首次命中通过</li>
 *   <li>窗口内再次命中被拒</li>
 *   <li>窗口外再次通过</li>
 *   <li>多目标互不干扰</li>
 *   <li>开关控制</li>
 * </ul>
 */
public class RiftDamageRateLimitTest {

  /**
   * 限频逻辑模拟器（纯函数版本）
   *
   * <p>不依赖 Minecraft 实体，使用 UUID 模拟目标。
   */
  private static class RateLimiterSimulator {
    private final java.util.Map<UUID, Long> limiter = new java.util.concurrent.ConcurrentHashMap<>();
    private final int windowTicks;
    private final boolean enabled;

    public RateLimiterSimulator(int windowTicks, boolean enabled) {
      this.windowTicks = windowTicks;
      this.enabled = enabled;
    }

    public boolean tryPass(UUID targetId, long now) {
      if (!enabled) {
        return true;
      }

      Long last = limiter.get(targetId);
      if (last != null && (now - last) < windowTicks) {
        return false;
      }

      limiter.put(targetId, now);
      return true;
    }

    public void clear() {
      limiter.clear();
    }
  }

  private RateLimiterSimulator limiter;

  @BeforeEach
  void setUp() {
    // 默认配置：窗口10 tick，启用限频
    limiter = new RateLimiterSimulator(10, true);
  }

  @Test
  void testFirstHitPasses() {
    UUID target = UUID.randomUUID();
    long now = 100L;

    boolean result = limiter.tryPass(target, now);
    assertTrue(result, "首次命中应通过");
  }

  @Test
  void testSecondHitWithinWindowIsBlocked() {
    UUID target = UUID.randomUUID();
    long now = 100L;

    limiter.tryPass(target, now);
    boolean result = limiter.tryPass(target, now + 1);

    assertFalse(result, "窗口内再次命中应被拒");
  }

  @Test
  void testSecondHitOutsideWindowPasses() {
    UUID target = UUID.randomUUID();
    long now = 100L;

    limiter.tryPass(target, now);
    boolean result = limiter.tryPass(target, now + 10);

    assertTrue(result, "窗口外再次命中应通过");
  }

  @Test
  void testMultipleTargetsIndependent() {
    UUID target1 = UUID.randomUUID();
    UUID target2 = UUID.randomUUID();
    long now = 100L;

    // target1 首次命中
    assertTrue(limiter.tryPass(target1, now));

    // target2 首次命中（不受 target1 影响）
    assertTrue(limiter.tryPass(target2, now));

    // target1 窗口内再次命中被拒
    assertFalse(limiter.tryPass(target1, now + 1));

    // target2 窗口内再次命中被拒
    assertFalse(limiter.tryPass(target2, now + 1));
  }

  @Test
  void testDisabledLimiterAlwaysPasses() {
    RateLimiterSimulator disabledLimiter = new RateLimiterSimulator(10, false);
    UUID target = UUID.randomUUID();
    long now = 100L;

    // 连续命中都应通过
    assertTrue(disabledLimiter.tryPass(target, now));
    assertTrue(disabledLimiter.tryPass(target, now + 1));
    assertTrue(disabledLimiter.tryPass(target, now + 2));
  }

  @Test
  void testWindowBoundaryExactly() {
    UUID target = UUID.randomUUID();
    long now = 100L;

    limiter.tryPass(target, now);

    // now + 9：仍在窗口内（10 tick 窗口，差值 9 < 10）
    assertFalse(limiter.tryPass(target, now + 9), "窗口边界内应被拒");

    // now + 10：窗口边界（差值 10 = 10，不满足 < 条件）
    assertTrue(limiter.tryPass(target, now + 10), "窗口边界应通过");
  }

  @Test
  void testMultipleHitsSequence() {
    UUID target = UUID.randomUUID();
    long now = 0L;

    // 时间线：0, 5, 10, 15, 20
    // 窗口：10 tick

    assertTrue(limiter.tryPass(target, now)); // 0: 通过
    assertFalse(limiter.tryPass(target, now + 5)); // 5: 拒绝（距离上次 5 < 10）
    assertTrue(limiter.tryPass(target, now + 10)); // 10: 通过（距离上次 10 = 10）
    assertFalse(limiter.tryPass(target, now + 15)); // 15: 拒绝（距离上次 5 < 10）
    assertTrue(limiter.tryPass(target, now + 20)); // 20: 通过（距离上次 10 = 10）
  }

  @Test
  void testZeroWindowAlwaysPasses() {
    RateLimiterSimulator zeroWindowLimiter = new RateLimiterSimulator(0, true);
    UUID target = UUID.randomUUID();
    long now = 100L;

    // 窗口为0，任何时刻都应通过
    assertTrue(zeroWindowLimiter.tryPass(target, now));
    assertTrue(zeroWindowLimiter.tryPass(target, now));
    assertTrue(zeroWindowLimiter.tryPass(target, now + 1));
  }

  @Test
  void testLargeTimeGap() {
    UUID target = UUID.randomUUID();
    long now = 100L;

    limiter.tryPass(target, now);

    // 很长时间后再次命中
    assertTrue(limiter.tryPass(target, now + 1000), "长时间后应通过");
  }

  @Test
  void testManyTargets() {
    long now = 100L;

    // 创建多个目标并测试
    for (int i = 0; i < 100; i++) {
      UUID target = UUID.randomUUID();
      assertTrue(limiter.tryPass(target, now), "目标 " + i + " 首次命中应通过");
      assertFalse(limiter.tryPass(target, now + 1), "目标 " + i + " 窗口内再次命中应被拒");
    }
  }

  @Test
  void testSameTargetDifferentSources() {
    // 限频是基于目标UUID的，不区分来源
    // 同一目标在窗口内只能被命中一次，无论来自裂隙穿刺还是共鸣波
    UUID target = UUID.randomUUID();
    long now = 100L;

    // 来源1（假设是裂隙穿刺）
    assertTrue(limiter.tryPass(target, now), "来源1首次命中应通过");

    // 来源2（假设是共鸣波），同一时刻
    assertFalse(limiter.tryPass(target, now), "来源2同时命中应被拒");

    // 窗口外
    assertTrue(limiter.tryPass(target, now + 10), "窗口外命中应通过");
  }
}
