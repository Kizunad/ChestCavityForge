package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;

/**
 * FxEngine Shutdown 测试
 *
 * <p>测试服务器停服时的统一收尾逻辑：
 * - shutdown() 方法停止所有活跃 Track
 * - 所有 Track 的 onStop 被调用且 reason 为 ENGINE_SHUTDOWN
 * - shutdown() 清空所有活跃 Track
 */
@DisplayName("FxEngine Shutdown 测试")
public class FxEngineShutdownTest {

  private FxTimelineEngine engine;
  private FxEngineConfig config;

  @BeforeEach
  void setUp() {
    engine = FxTimelineEngine.getInstance();
    config = FxEngine.getConfig();

    // 配置：引擎启用，预算关闭
    config.enabled = true;
    config.budgetEnabled = false;
  }

  @Test
  @DisplayName("shutdown() 停止所有活跃 Track")
  void testShutdownStopsAllTracks() {
    int initialActiveCount = engine.getActiveCount();

    // 注册 3 个 Track
    FxTrack track1 = createMockTrack("shutdown-1", null, 100);
    FxTrack track2 = createMockTrack("shutdown-2", null, 100);
    FxTrack track3 = createMockTrack("shutdown-3", null, 100);

    engine.register(track1, null);
    engine.register(track2, null);
    engine.register(track3, null);

    assertEquals(initialActiveCount + 3, engine.getActiveCount(), "应该有 3 个新的活跃 Track");

    // 调用 shutdown
    engine.shutdown(null);

    // 验证所有 Track 都被停止
    assertEquals(initialActiveCount, engine.getActiveCount(), "shutdown 后应该没有活跃 Track");
  }

  @Test
  @DisplayName("shutdown() 触发 onStop 回调且 reason 为 ENGINE_SHUTDOWN")
  void testShutdownCallsOnStop() {
    AtomicInteger onStopCount = new AtomicInteger(0);
    AtomicInteger shutdownReasonCount = new AtomicInteger(0);

    // 创建带计数器的 Track
    FxTrack track =
        new FxTrack() {
          @Override
          public String getId() {
            return "stop-callback-test";
          }

          @Override
          public int getTtlTicks() {
            return 100;
          }

          @Override
          public void onStart(ServerLevel level) {}

          @Override
          public void onTick(ServerLevel level, int elapsedTicks) {}

          @Override
          public void onStop(ServerLevel level, StopReason reason) {
            onStopCount.incrementAndGet();
            if (reason == StopReason.ENGINE_SHUTDOWN) {
              shutdownReasonCount.incrementAndGet();
            }
          }
        };

    engine.register(track, null);

    // 调用 shutdown
    engine.shutdown(null);

    // 验证 onStop 被调用一次，且 reason 为 ENGINE_SHUTDOWN
    assertEquals(1, onStopCount.get(), "onStop 应该被调用一次");
    assertEquals(1, shutdownReasonCount.get(), "StopReason 应该是 ENGINE_SHUTDOWN");
  }

  @Test
  @DisplayName("shutdown() 空引擎不抛异常")
  void testShutdownEmptyEngine() {
    // 清空所有 Track（通过 shutdown）
    engine.shutdown(null);

    // 再次调用 shutdown 不应该抛异常
    assertDoesNotThrow(() -> engine.shutdown(null), "空引擎 shutdown 不应该抛异常");
  }

  @Test
  @DisplayName("shutdown() 多次调用安全")
  void testShutdownMultipleTimes() {
    // 注册一个 Track
    FxTrack track = createMockTrack("multi-shutdown", null, 100);
    engine.register(track, null);

    // 第一次 shutdown
    engine.shutdown(null);
    int activeCountAfterFirst = engine.getActiveCount();

    // 第二次 shutdown
    engine.shutdown(null);
    int activeCountAfterSecond = engine.getActiveCount();

    // 两次 shutdown 后活跃数应该相同
    assertEquals(activeCountAfterFirst, activeCountAfterSecond, "多次 shutdown 应该安全");
  }

  @Test
  @DisplayName("shutdown() 处理异常不影响其他 Track")
  void testShutdownHandlesExceptions() {
    AtomicInteger onStopCount = new AtomicInteger(0);

    // Track 1: onStop 抛异常
    FxTrack track1 =
        new FxTrack() {
          @Override
          public String getId() {
            return "exception-track";
          }

          @Override
          public int getTtlTicks() {
            return 100;
          }

          @Override
          public void onStart(ServerLevel level) {}

          @Override
          public void onTick(ServerLevel level, int elapsedTicks) {}

          @Override
          public void onStop(ServerLevel level, StopReason reason) {
            throw new RuntimeException("Test exception in onStop");
          }
        };

    // Track 2: 正常 onStop
    FxTrack track2 =
        new FxTrack() {
          @Override
          public String getId() {
            return "normal-track";
          }

          @Override
          public int getTtlTicks() {
            return 100;
          }

          @Override
          public void onStart(ServerLevel level) {}

          @Override
          public void onTick(ServerLevel level, int elapsedTicks) {}

          @Override
          public void onStop(ServerLevel level, StopReason reason) {
            onStopCount.incrementAndGet();
          }
        };

    engine.register(track1, null);
    engine.register(track2, null);

    // shutdown 应该不抛异常，且 track2 的 onStop 应该被调用
    assertDoesNotThrow(() -> engine.shutdown(null), "shutdown 应该处理 onStop 异常");

    // track2 的 onStop 应该被调用
    assertEquals(1, onStopCount.get(), "正常 Track 的 onStop 应该被调用");
  }

  // ========== 辅助方法 ==========

  private FxTrack createMockTrack(String id, UUID ownerId, int ttlTicks) {
    return new FxTrack() {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public int getTtlTicks() {
        return ttlTicks;
      }

      @Override
      public UUID getOwnerId() {
        return ownerId;
      }

      @Override
      public void onStart(ServerLevel level) {}

      @Override
      public void onTick(ServerLevel level, int elapsedTicks) {}

      @Override
      public void onStop(ServerLevel level, StopReason reason) {}
    };
  }
}
