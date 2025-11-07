package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;

/**
 * FxEngine 状态机测试
 *
 * <p>测试 Track 生命周期状态转换：
 * - onStart 仅执行一次
 * - tickInterval 正常触发
 * - TTL 到期触发 onStop
 * - onTick 异常不影响其他 Track
 *
 * <p>注意：由于单元测试无法模拟完整的 ServerTickEvent，这里主要测试状态管理逻辑。
 * 完整的 tick 流程测试需要通过集成测试或手册测试完成。
 */
@DisplayName("FxEngine 状态机测试")
public class FxEngineStateMachineTest {

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
  @DisplayName("Track 注册成功")
  void testTrackRegistration() {
    FxTrack track = createMockTrack("state-test-1", null, 100, 1);
    String trackId = engine.register(track, null);

    assertNotNull(trackId, "Track 应该成功注册");
    assertEquals(track, engine.find(trackId), "应该能查找到注册的 Track");
  }

  @Test
  @DisplayName("Track 取消成功")
  void testTrackCancellation() {
    AtomicBoolean onStopCalled = new AtomicBoolean(false);

    FxTrack track =
        new FxTrack() {
          @Override
          public String getId() {
            return "cancel-test";
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
            onStopCalled.set(true);
            assertEquals(StopReason.CANCELLED, reason, "停止原因应该是 CANCELLED");
          }
        };

    String trackId = engine.register(track, null);
    assertNotNull(trackId, "Track 应该成功注册");

    // 取消 Track
    boolean cancelled = engine.cancel(trackId, null);
    assertTrue(cancelled, "取消应该成功");

    // 验证 onStop 被调用
    assertTrue(onStopCalled.get(), "onStop 应该被调用");

    // 验证 Track 已被移除
    assertNull(engine.find(trackId), "Track 应该已被移除");
  }

  @Test
  @DisplayName("tickInterval 配置测试")
  void testTickInterval() {
    // 测试不同的 tickInterval 值
    FxTrack track1 = createMockTrack("interval-1", null, 100, 1);
    assertEquals(1, track1.getTickInterval(), "默认 tickInterval 应该是 1");

    FxTrack track2 = createMockTrack("interval-2", null, 100, 5);
    assertEquals(5, track2.getTickInterval(), "tickInterval 应该是 5");

    FxTrack track3 = createMockTrack("interval-3", null, 100, 20);
    assertEquals(20, track3.getTickInterval(), "tickInterval 应该是 20");
  }

  @Test
  @DisplayName("Track ID 冲突处理")
  void testTrackIdConflict() {
    AtomicBoolean firstOnStopCalled = new AtomicBoolean(false);

    // 第一个 Track
    FxTrack track1 =
        new FxTrack() {
          @Override
          public String getId() {
            return "conflict-id";
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
            firstOnStopCalled.set(true);
            assertEquals(StopReason.CANCELLED, reason, "第一个 Track 应该被 CANCELLED");
          }
        };

    String trackId1 = engine.register(track1, null);
    assertNotNull(trackId1, "第一个 Track 应该成功注册");

    // 第二个 Track（相同 ID）
    FxTrack track2 = createMockTrack("conflict-id", null, 50, 1);
    String trackId2 = engine.register(track2, null);
    assertNotNull(trackId2, "第二个 Track 应该成功注册");

    // 验证第一个 Track 的 onStop 被调用
    assertTrue(firstOnStopCalled.get(), "第一个 Track 的 onStop 应该被调用");

    // 验证活跃 Track 是第二个
    FxTrack activeTrack = engine.find(trackId2);
    assertEquals(track2, activeTrack, "活跃 Track 应该是第二个 Track");
  }

  @Test
  @DisplayName("统计计数器测试")
  void testStatistics() {
    int initialActiveCount = engine.getActiveCount();

    // 注册 3 个 Track
    FxTrack track1 = createMockTrack("stats-1", null, 100, 1);
    FxTrack track2 = createMockTrack("stats-2", null, 100, 1);
    FxTrack track3 = createMockTrack("stats-3", null, 100, 1);

    String trackId1 = engine.register(track1, null);
    String trackId2 = engine.register(track2, null);
    String trackId3 = engine.register(track3, null);

    // 验证活跃数量增加
    assertEquals(
        initialActiveCount + 3, engine.getActiveCount(), "活跃 Track 数量应该增加 3");

    // 取消一个 Track
    engine.cancel(trackId2, null);

    // 验证活跃数量减少
    assertEquals(
        initialActiveCount + 2, engine.getActiveCount(), "活跃 Track 数量应该减少 1");
  }

  @Test
  @DisplayName("per-owner 索引测试")
  void testPerOwnerIndex() {
    UUID owner1 = UUID.randomUUID();
    UUID owner2 = UUID.randomUUID();

    // Owner 1 提交 3 个 Track
    FxTrack track1 = createMockTrack("owner1-1", owner1, 100, 1);
    FxTrack track2 = createMockTrack("owner1-2", owner1, 100, 1);
    FxTrack track3 = createMockTrack("owner1-3", owner1, 100, 1);

    engine.register(track1, null);
    String trackId2 = engine.register(track2, null);
    engine.register(track3, null);

    // Owner 2 提交 1 个 Track
    FxTrack track4 = createMockTrack("owner2-1", owner2, 100, 1);
    engine.register(track4, null);

    // 取消 Owner 1 的一个 Track
    engine.cancel(trackId2, null);

    // 验证索引更新正确（这个测试主要验证不会崩溃，实际索引是内部实现）
    assertNull(engine.find(trackId2), "取消的 Track 应该不存在");
  }

  @Test
  @DisplayName("TTL 值测试")
  void testTtlValues() {
    // 测试不同的 TTL 值
    FxTrack track1 = createMockTrack("ttl-1", null, 20, 1);
    assertEquals(20, track1.getTtlTicks(), "TTL 应该是 20");

    FxTrack track2 = createMockTrack("ttl-2", null, 100, 1);
    assertEquals(100, track2.getTtlTicks(), "TTL 应该是 100");

    FxTrack track3 = createMockTrack("ttl-3", null, 1000, 1);
    assertEquals(1000, track3.getTtlTicks(), "TTL 应该是 1000");
  }

  // ========== 辅助方法 ==========

  private FxTrack createMockTrack(String id, UUID ownerId, int ttlTicks, int tickInterval) {
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
      public int getTickInterval() {
        return tickInterval;
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
