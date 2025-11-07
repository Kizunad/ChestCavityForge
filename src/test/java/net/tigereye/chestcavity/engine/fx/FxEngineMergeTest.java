package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * FxEngine 合并策略测试
 *
 * <p>测试合并策略的核心逻辑：
 * - EXTEND_TTL：延长现有 Track 的 TTL
 * - DROP：丢弃新的 Track 请求
 * - REPLACE：替换现有 Track
 * - 同 mergeKey 重复提交行为
 */
@DisplayName("FxEngine 合并策略测试")
public class FxEngineMergeTest {

  private FxTimelineEngine engine;
  private FxEngineConfig config;

  @BeforeEach
  void setUp() {
    engine = FxTimelineEngine.getInstance();
    config = FxEngine.getConfig();

    // 配置：引擎启用，预算关闭（简化测试）
    config.enabled = true;
    config.budgetEnabled = false;
  }

  @Test
  @DisplayName("EXTEND_TTL 策略：延长现有 Track 的 TTL")
  void testExtendTtlStrategy() {
    String mergeKey = "test-merge-key-extend";
    int initialMergeCount = engine.getMergeCount();

    // 第一次提交：TTL = 100
    FxTrack track1 = createMockTrack("track-1", null, 100);
    FxTrackSpec spec1 =
        FxTrackSpec.builder("track-1")
            .ttl(100)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.EXTEND_TTL)
            .onStart(track1::onStart)
            .onTick(track1::onTick)
            .onStop(track1::onStop)
            .build();

    String trackId1 = engine.register(track1, spec1);
    assertNotNull(trackId1, "第一次提交应该成功");

    // 第二次提交：相同 mergeKey，TTL = 50
    FxTrack track2 = createMockTrack("track-2", null, 50);
    FxTrackSpec spec2 =
        FxTrackSpec.builder("track-2")
            .ttl(50)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.EXTEND_TTL)
            .onStart(track2::onStart)
            .onTick(track2::onTick)
            .onStop(track2::onStop)
            .build();

    String trackId2 = engine.register(track2, spec2);
    assertEquals(trackId1, trackId2, "EXTEND_TTL 策略应该返回原 Track ID");

    // 验证合并计数增加
    assertEquals(initialMergeCount + 1, engine.getMergeCount(), "应该触发一次合并");

    // 验证只有一个活跃 Track
    FxTrack activeTrack = engine.find(trackId1);
    assertNotNull(activeTrack, "原 Track 应该仍然存在");
    assertEquals(track1, activeTrack, "活跃 Track 应该是第一个提交的 Track");
  }

  @Test
  @DisplayName("DROP 策略：丢弃新的 Track 请求")
  void testDropStrategy() {
    String mergeKey = "test-merge-key-drop";
    int initialDropCount = engine.getDropCount();

    // 第一次提交
    FxTrack track1 = createMockTrack("drop-track-1", null, 100);
    FxTrackSpec spec1 =
        FxTrackSpec.builder("drop-track-1")
            .ttl(100)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.DROP)
            .onStart(track1::onStart)
            .onTick(track1::onTick)
            .onStop(track1::onStop)
            .build();

    String trackId1 = engine.register(track1, spec1);
    assertNotNull(trackId1, "第一次提交应该成功");

    // 第二次提交：相同 mergeKey，应该被丢弃
    FxTrack track2 = createMockTrack("drop-track-2", null, 50);
    FxTrackSpec spec2 =
        FxTrackSpec.builder("drop-track-2")
            .ttl(50)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.DROP)
            .onStart(track2::onStart)
            .onTick(track2::onTick)
            .onStop(track2::onStop)
            .build();

    String trackId2 = engine.register(track2, spec2);
    assertNull(trackId2, "DROP 策略应该丢弃新的 Track");

    // 验证丢弃计数增加
    assertEquals(initialDropCount + 1, engine.getDropCount(), "应该触发一次丢弃");

    // 验证只有第一个 Track 存在
    FxTrack activeTrack = engine.find(trackId1);
    assertNotNull(activeTrack, "原 Track 应该仍然存在");
    assertEquals(track1, activeTrack, "活跃 Track 应该是第一个提交的 Track");
  }

  @Test
  @DisplayName("REPLACE 策略：替换现有 Track")
  void testReplaceStrategy() {
    String mergeKey = "test-merge-key-replace";
    int initialReplaceCount = engine.getReplaceCount();

    // 第一次提交
    FxTrack track1 = createMockTrack("replace-track-1", null, 100);
    FxTrackSpec spec1 =
        FxTrackSpec.builder("replace-track-1")
            .ttl(100)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.REPLACE)
            .onStart(track1::onStart)
            .onTick(track1::onTick)
            .onStop(track1::onStop)
            .build();

    String trackId1 = engine.register(track1, spec1);
    assertNotNull(trackId1, "第一次提交应该成功");

    // 第二次提交：相同 mergeKey，应该替换
    FxTrack track2 = createMockTrack("replace-track-2", null, 50);
    FxTrackSpec spec2 =
        FxTrackSpec.builder("replace-track-2")
            .ttl(50)
            .mergeKey(mergeKey)
            .mergeStrategy(MergeStrategy.REPLACE)
            .onStart(track2::onStart)
            .onTick(track2::onTick)
            .onStop(track2::onStop)
            .build();

    String trackId2 = engine.register(track2, spec2);
    assertNotNull(trackId2, "REPLACE 策略应该注册新的 Track");
    assertNotEquals(trackId1, trackId2, "新 Track ID 应该不同");

    // 验证替换计数增加
    assertEquals(initialReplaceCount + 1, engine.getReplaceCount(), "应该触发一次替换");

    // 验证原 Track 已被移除
    FxTrack oldTrack = engine.find(trackId1);
    assertNull(oldTrack, "原 Track 应该已被移除");

    // 验证新 Track 存在
    FxTrack newTrack = engine.find(trackId2);
    assertNotNull(newTrack, "新 Track 应该存在");
    assertEquals(track2, newTrack, "活跃 Track 应该是第二个提交的 Track");
  }

  @Test
  @DisplayName("无 mergeKey：允许重复提交")
  void testNoMergeKey() {
    // 提交两个没有 mergeKey 的 Track
    FxTrack track1 = createMockTrack("no-merge-1", null, 100);
    String trackId1 = engine.register(track1, null);
    assertNotNull(trackId1, "第一次提交应该成功");

    FxTrack track2 = createMockTrack("no-merge-2", null, 50);
    String trackId2 = engine.register(track2, null);
    assertNotNull(trackId2, "第二次提交应该成功");

    // 验证两个 Track 都存在
    assertNotNull(engine.find(trackId1), "第一个 Track 应该存在");
    assertNotNull(engine.find(trackId2), "第二个 Track 应该存在");
    assertNotEquals(trackId1, trackId2, "两个 Track ID 应该不同");
  }

  @Test
  @DisplayName("不同 mergeKey：不触发合并")
  void testDifferentMergeKey() {
    int initialMergeCount = engine.getMergeCount();

    // 提交两个不同 mergeKey 的 Track
    FxTrack track1 = createMockTrack("diff-merge-1", null, 100);
    FxTrackSpec spec1 =
        FxTrackSpec.builder("diff-merge-1")
            .ttl(100)
            .mergeKey("key-1")
            .mergeStrategy(MergeStrategy.EXTEND_TTL)
            .onStart(track1::onStart)
            .onTick(track1::onTick)
            .onStop(track1::onStop)
            .build();

    String trackId1 = engine.register(track1, spec1);
    assertNotNull(trackId1, "第一次提交应该成功");

    FxTrack track2 = createMockTrack("diff-merge-2", null, 50);
    FxTrackSpec spec2 =
        FxTrackSpec.builder("diff-merge-2")
            .ttl(50)
            .mergeKey("key-2")
            .mergeStrategy(MergeStrategy.EXTEND_TTL)
            .onStart(track2::onStart)
            .onTick(track2::onTick)
            .onStop(track2::onStop)
            .build();

    String trackId2 = engine.register(track2, spec2);
    assertNotNull(trackId2, "第二次提交应该成功");

    // 验证不触发合并
    assertEquals(initialMergeCount, engine.getMergeCount(), "不同 mergeKey 不应该触发合并");

    // 验证两个 Track 都存在
    assertNotNull(engine.find(trackId1), "第一个 Track 应该存在");
    assertNotNull(engine.find(trackId2), "第二个 Track 应该存在");
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
