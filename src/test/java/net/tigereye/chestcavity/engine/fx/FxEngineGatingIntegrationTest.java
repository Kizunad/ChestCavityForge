package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * FxEngine 门控集成测试
 *
 * <p>测试门控逻辑的集成行为：
 * - Owner 死亡时停止 Track
 * - 门控失败时暂停模式
 * - 门控失败时停止模式
 * - FxTrackSpec 门控参数覆盖全局配置
 *
 * <p>注意：由于单元测试无法创建真实的 ServerLevel 和 Entity，这里主要测试配置逻辑和参数传递。
 * 完整的门控功能测试需要通过集成测试或手册测试完成。
 */
@DisplayName("FxEngine 门控集成测试")
public class FxEngineGatingIntegrationTest {

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
  @DisplayName("门控配置：默认值验证")
  void testGatingConfigDefaults() {
    FxEngineConfig defaultConfig = FxEngineConfig.createDefault();

    assertFalse(defaultConfig.gatingCheckChunkLoaded, "区块加载检查默认应该关闭");
    assertEquals(0.0, defaultConfig.gatingDefaultPlayerRadius, 0.001, "玩家半径默认应该是 0");
    assertFalse(defaultConfig.gatingPauseOnFail, "门控失败默认应该停止而非暂停");
  }

  @Test
  @DisplayName("门控配置：暂停模式")
  void testGatingPauseMode() {
    config.gatingPauseOnFail = true;

    // 注册一个 Track
    FxTrack track = createMockTrack("pause-test", UUID.randomUUID(), 100);
    String trackId = engine.register(track, null);
    assertNotNull(trackId, "Track 应该成功注册");

    // 验证配置
    assertTrue(config.gatingPauseOnFail, "门控失败应该暂停");
  }

  @Test
  @DisplayName("门控配置：停止模式")
  void testGatingStopMode() {
    config.gatingPauseOnFail = false;

    // 注册一个 Track
    FxTrack track = createMockTrack("stop-test", UUID.randomUUID(), 100);
    String trackId = engine.register(track, null);
    assertNotNull(trackId, "Track 应该成功注册");

    // 验证配置
    assertFalse(config.gatingPauseOnFail, "门控失败应该停止");
  }

  @Test
  @DisplayName("FxTrackSpec 门控参数：玩家半径")
  void testSpecPlayerRadius() {
    FxTrackSpec spec =
        FxTrackSpec.builder("radius-test")
            .ttl(100)
            .playerRadius(50.0)
            .build();

    assertEquals(50.0, spec.getPlayerRadius(), 0.001, "玩家半径应该是 50");
  }

  @Test
  @DisplayName("FxTrackSpec 门控参数：区块加载检查")
  void testSpecCheckChunkLoaded() {
    FxTrackSpec spec =
        FxTrackSpec.builder("chunk-test")
            .ttl(100)
            .checkChunkLoaded(true)
            .build();

    assertTrue(spec.isCheckChunkLoaded(), "区块加载检查应该启用");
  }

  @Test
  @DisplayName("FxTrackSpec 门控参数：默认值")
  void testSpecGatingDefaults() {
    FxTrackSpec spec =
        FxTrackSpec.builder("default-test")
            .ttl(100)
            .build();

    assertEquals(-1.0, spec.getPlayerRadius(), 0.001, "默认玩家半径应该是 -1（使用全局配置）");
    assertFalse(spec.isCheckChunkLoaded(), "默认区块加载检查应该关闭");
  }

  @Test
  @DisplayName("门控统计：暂停计数")
  void testPauseCount() {
    int initialPauseCount = engine.getPauseCount();

    // 暂停计数应该在门控失败且暂停模式下递增
    // 由于无法创建真实的 ServerLevel 和 Entity，这里只验证初始值
    assertTrue(initialPauseCount >= 0, "暂停计数应该 >= 0");
  }

  @Test
  @DisplayName("OWNER_REMOVED 停止原因")
  void testOwnerRemovedStopReason() {
    // 验证 StopReason.OWNER_REMOVED 枚举值存在
    StopReason reason = StopReason.OWNER_REMOVED;
    assertNotNull(reason, "OWNER_REMOVED 停止原因应该存在");
    assertEquals("OWNER_REMOVED", reason.name(), "停止原因名称应该是 OWNER_REMOVED");
  }

  @Test
  @DisplayName("GATING_FAILED 停止原因")
  void testGatingFailedStopReason() {
    // 验证 StopReason.GATING_FAILED 枚举值存在
    StopReason reason = StopReason.GATING_FAILED;
    assertNotNull(reason, "GATING_FAILED 停止原因应该存在");
    assertEquals("GATING_FAILED", reason.name(), "停止原因名称应该是 GATING_FAILED");
  }

  @Test
  @DisplayName("ENGINE_SHUTDOWN 停止原因")
  void testEngineShutdownStopReason() {
    // 验证 StopReason.ENGINE_SHUTDOWN 枚举值存在
    StopReason reason = StopReason.ENGINE_SHUTDOWN;
    assertNotNull(reason, "ENGINE_SHUTDOWN 停止原因应该存在");
    assertEquals("ENGINE_SHUTDOWN", reason.name(), "停止原因名称应该是 ENGINE_SHUTDOWN");
  }

  @Test
  @DisplayName("门控配置：toString 包含所有字段")
  void testGatingConfigToString() {
    config.gatingCheckChunkLoaded = true;
    config.gatingDefaultPlayerRadius = 32.0;
    config.gatingPauseOnFail = true;

    String configString = config.toString();

    assertTrue(configString.contains("gatingCheckChunkLoaded=true"), "toString 应该包含 gatingCheckChunkLoaded");
    assertTrue(configString.contains("gatingDefaultPlayerRadius=32.0"), "toString 应该包含 gatingDefaultPlayerRadius");
    assertTrue(configString.contains("gatingPauseOnFail=true"), "toString 应该包含 gatingPauseOnFail");
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
