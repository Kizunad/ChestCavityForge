package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * FxEngine 预算控制测试
 *
 * <p>测试预算控制的核心逻辑：
 * - 预算关闭：无限提交不触发丢弃
 * - 预算开启：提交 > perLevel 上限 → 触发丢弃
 * - per-owner 上限检查
 */
@DisplayName("FxEngine 预算控制测试")
public class FxEngineBudgetTest {

  private FxTimelineEngine engine;
  private FxEngineConfig config;

  @BeforeEach
  void setUp() {
    engine = FxTimelineEngine.getInstance();
    config = FxEngine.getConfig();

    // 清理统计计数（注意：这是全局单例，可能影响其他测试）
    // 在实际项目中，应该考虑使用依赖注入来避免单例问题
  }

  @Test
  @DisplayName("预算关闭：无限提交不触发丢弃")
  void testBudgetDisabled() {
    // 配置：引擎启用，但预算关闭
    config.enabled = true;
    config.budgetEnabled = false;

    int initialDropCount = engine.getDropCount();

    // 提交 100 个 Track（远超默认上限）
    for (int i = 0; i < 100; i++) {
      FxTrack track = createMockTrack("track-" + i, null, 100);
      String trackId = engine.register(track, null);
      assertNotNull(trackId, "预算关闭时应该能注册所有 Track");
    }

    // 验证没有触发丢弃
    assertEquals(initialDropCount, engine.getDropCount(), "预算关闭时不应该丢弃任何 Track");
  }

  @Test
  @DisplayName("预算开启：提交 > perLevel 上限 → 触发丢弃")
  void testPerLevelCapExceeded() {
    // 配置：引擎启用，预算开启，per-level 上限设为 10
    config.enabled = true;
    config.budgetEnabled = true;
    config.perLevelCap = 10;

    // 清理现有 Track
    int initialActiveCount = engine.getActiveCount();

    int initialDropCount = engine.getDropCount();

    // 提交 15 个 Track（超过上限）
    int successCount = 0;
    for (int i = 0; i < 15; i++) {
      FxTrack track = createMockTrack("budget-track-" + i, null, 100);
      String trackId = engine.register(track, null);
      if (trackId != null) {
        successCount++;
      }
    }

    // 验证：前 (10 - initialActiveCount) 个应该成功，剩余的应该被丢弃
    int expectedSuccessCount = Math.min(15, config.perLevelCap - initialActiveCount);
    int expectedDropCount = 15 - expectedSuccessCount;

    assertTrue(
        engine.getDropCount() >= initialDropCount + expectedDropCount,
        "超过 per-level 上限应该触发丢弃");
  }

  @Test
  @DisplayName("预算开启：per-owner 上限检查")
  void testPerOwnerCapExceeded() {
    // 配置：引擎启用，预算开启，per-owner 上限设为 5
    config.enabled = true;
    config.budgetEnabled = true;
    config.perLevelCap = 100; // 设置足够大的 per-level 上限
    config.perOwnerCap = 5;

    UUID owner1 = UUID.randomUUID();
    UUID owner2 = UUID.randomUUID();

    int initialDropCount = engine.getDropCount();

    // Owner 1 提交 7 个 Track（超过上限）
    int owner1SuccessCount = 0;
    for (int i = 0; i < 7; i++) {
      FxTrack track = createMockTrack("owner1-track-" + i, owner1, 100);
      String trackId = engine.register(track, null);
      if (trackId != null) {
        owner1SuccessCount++;
      }
    }

    // Owner 2 提交 3 个 Track（未超过上限）
    int owner2SuccessCount = 0;
    for (int i = 0; i < 3; i++) {
      FxTrack track = createMockTrack("owner2-track-" + i, owner2, 100);
      String trackId = engine.register(track, null);
      if (trackId != null) {
        owner2SuccessCount++;
      }
    }

    // 验证：Owner 1 应该只成功 5 个，丢弃 2 个；Owner 2 应该成功 3 个
    assertTrue(
        owner1SuccessCount <= config.perOwnerCap,
        "Owner 1 成功数量应该不超过 per-owner 上限");
    assertEquals(3, owner2SuccessCount, "Owner 2 应该成功提交 3 个 Track");

    int expectedDropCount = 7 - owner1SuccessCount;
    assertTrue(
        engine.getDropCount() >= initialDropCount + expectedDropCount,
        "超过 per-owner 上限应该触发丢弃");
  }

  @Test
  @DisplayName("引擎关闭：所有提交被静默丢弃")
  void testEngineDisabled() {
    // 配置：引擎关闭
    config.enabled = false;

    int initialDropCount = engine.getDropCount();

    // 提交 10 个 Track
    for (int i = 0; i < 10; i++) {
      FxTrack track = createMockTrack("disabled-track-" + i, null, 100);
      String trackId = engine.register(track, null);
      assertNull(trackId, "引擎关闭时应该返回 null");
    }

    // 验证：所有 Track 都被丢弃
    assertEquals(
        initialDropCount + 10, engine.getDropCount(), "引擎关闭时应该丢弃所有 Track");
  }

  // ========== 辅助方法 ==========

  /**
   * 创建一个 Mock FxTrack 实例（不依赖 Minecraft 世界对象）。
   *
   * @param id Track ID
   * @param ownerId Owner ID（可为 null）
   * @param ttlTicks TTL tick 数
   * @return Mock Track 实例
   */
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
      public void onStart(ServerLevel level) {
        // Mock: 不执行任何操作
      }

      @Override
      public void onTick(ServerLevel level, int elapsedTicks) {
        // Mock: 不执行任何操作
      }

      @Override
      public void onStop(ServerLevel level, StopReason reason) {
        // Mock: 不执行任何操作
      }
    };
  }
}
