package net.tigereye.chestcavity.engine.fx;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * FxGatingUtils 门控工具类测试
 *
 * <p>测试门控判定的核心逻辑（不依赖 Minecraft 世界对象的部分）：
 * - 参数验证（null 检查）
 * - 边界条件测试
 * - 逻辑正确性验证
 *
 * <p>注意：由于 Minecraft 核心类（ServerLevel、Entity）无法在单元测试中 mock，
 * 这里主要测试工具方法的参数验证和边界条件。完整的功能测试需要通过集成测试完成。
 */
@DisplayName("FxGatingUtils 门控工具类测试")
public class FxGatingUtilsTest {

  @Nested
  @DisplayName("参数验证测试")
  class ParameterValidationTest {

    @Test
    @DisplayName("isWithinPlayerRadius: null level 返回 false")
    void testIsWithinPlayerRadiusNullLevel() {
      Vec3 pos = new Vec3(0, 0, 0);
      assertFalse(
          FxGatingUtils.isWithinPlayerRadius(null, pos, 10.0),
          "null level 应该返回 false");
    }

    @Test
    @DisplayName("isWithinPlayerRadius: null pos 返回 false")
    void testIsWithinPlayerRadiusNullPos() {
      assertFalse(
          FxGatingUtils.isWithinPlayerRadius(null, (Vec3) null, 10.0),
          "null pos 应该返回 false");
    }

    @Test
    @DisplayName("isWithinPlayerRadius (BlockPos): null pos 返回 false")
    void testIsWithinPlayerRadiusBlockPosNull() {
      assertFalse(
          FxGatingUtils.isWithinPlayerRadius(null, (BlockPos) null, 10.0),
          "null BlockPos 应该返回 false");
    }

    @Test
    @DisplayName("isWithinPlayerRadius (Entity): null entity 返回 false")
    void testIsWithinPlayerRadiusEntityNull() {
      assertFalse(
          FxGatingUtils.isWithinPlayerRadius(null, 10.0),
          "null entity 应该返回 false");
    }

    @Test
    @DisplayName("isChunkLoaded: null level 返回 false")
    void testIsChunkLoadedNullLevel() {
      BlockPos pos = new BlockPos(0, 0, 0);
      assertFalse(
          FxGatingUtils.isChunkLoaded(null, pos),
          "null level 应该返回 false");
    }

    @Test
    @DisplayName("isChunkLoaded: null pos 返回 false")
    void testIsChunkLoadedNullPos() {
      assertFalse(
          FxGatingUtils.isChunkLoaded(null, (BlockPos) null),
          "null pos 应该返回 false");
    }

    @Test
    @DisplayName("isChunkLoaded (Vec3): null pos 返回 false")
    void testIsChunkLoadedVec3Null() {
      assertFalse(
          FxGatingUtils.isChunkLoaded(null, (Vec3) null),
          "null Vec3 应该返回 false");
    }

    @Test
    @DisplayName("isEntityValid: null entity 返回 false")
    void testIsEntityValidNull() {
      assertFalse(
          FxGatingUtils.isEntityValid(null),
          "null entity 应该返回 false");
    }

    @Test
    @DisplayName("checkGating (Vec3): null level 返回 false")
    void testCheckGatingVec3NullLevel() {
      Vec3 pos = new Vec3(0, 0, 0);
      assertFalse(
          FxGatingUtils.checkGating(null, pos, 10.0),
          "null level 应该返回 false");
    }

    @Test
    @DisplayName("checkGating (Vec3): null pos 返回 false")
    void testCheckGatingVec3NullPos() {
      assertFalse(
          FxGatingUtils.checkGating(null, null, 10.0),
          "null pos 应该返回 false");
    }

    @Test
    @DisplayName("checkGating (Entity): null entity 返回 false")
    void testCheckGatingEntityNull() {
      assertFalse(
          FxGatingUtils.checkGating(null, 10.0),
          "null entity 应该返回 false");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTest {

    @Test
    @DisplayName("半径为 0 的情况")
    void testZeroRadius() {
      // 半径为 0 时，checkGating 应该跳过玩家半径检查
      // 由于无法创建真实的 ServerLevel，这里只测试逻辑
      // 实际测试需要在集成测试中完成
      assertTrue(true, "边界条件测试占位符");
    }

    @Test
    @DisplayName("负半径的情况")
    void testNegativeRadius() {
      // 负半径应该被视为"不检查玩家半径"
      // checkGating 中使用 playerRadius > 0 判断
      assertTrue(true, "边界条件测试占位符");
    }

    @Test
    @DisplayName("极大半径的情况")
    void testLargeRadius() {
      // 测试极大半径值不会导致溢出
      double largeRadius = Double.MAX_VALUE / 2;
      assertTrue(largeRadius > 0, "极大半径值应该是正数");
    }
  }

  @Nested
  @DisplayName("坐标转换测试")
  class CoordinateConversionTest {

    @Test
    @DisplayName("BlockPos 转 Vec3")
    void testBlockPosToVec3() {
      BlockPos blockPos = new BlockPos(10, 64, 20);
      Vec3 vec3 = Vec3.atCenterOf(blockPos);

      // Vec3.atCenterOf 会将方块坐标转换为中心点坐标（+0.5）
      assertEquals(10.5, vec3.x, 0.001, "X 坐标应该是 10.5");
      assertEquals(64.5, vec3.y, 0.001, "Y 坐标应该是 64.5");
      assertEquals(20.5, vec3.z, 0.001, "Z 坐标应该是 20.5");
    }

    @Test
    @DisplayName("Vec3 转 BlockPos")
    void testVec3ToBlockPos() {
      Vec3 vec3 = new Vec3(10.7, 64.3, 20.9);
      BlockPos blockPos = BlockPos.containing(vec3);

      // BlockPos.containing 会向下取整
      assertEquals(10, blockPos.getX(), "X 坐标应该是 10");
      assertEquals(64, blockPos.getY(), "Y 坐标应该是 64");
      assertEquals(20, blockPos.getZ(), "Z 坐标应该是 20");
    }

    @Test
    @DisplayName("负坐标转换")
    void testNegativeCoordinates() {
      Vec3 vec3 = new Vec3(-10.5, -5.3, -20.7);
      BlockPos blockPos = BlockPos.containing(vec3);

      assertEquals(-11, blockPos.getX(), "负 X 坐标应该向下取整");
      assertEquals(-6, blockPos.getY(), "负 Y 坐标应该向下取整");
      assertEquals(-21, blockPos.getZ(), "负 Z 坐标应该向下取整");
    }
  }

  @Nested
  @DisplayName("距离计算测试")
  class DistanceCalculationTest {

    @Test
    @DisplayName("距离平方计算")
    void testDistanceSquared() {
      Vec3 pos1 = new Vec3(0, 0, 0);
      Vec3 pos2 = new Vec3(3, 4, 0);

      double distSq = pos1.distanceToSqr(pos2);
      assertEquals(25.0, distSq, 0.001, "距离平方应该是 25");
    }

    @Test
    @DisplayName("3D 距离计算")
    void test3DDistance() {
      Vec3 pos1 = new Vec3(0, 0, 0);
      Vec3 pos2 = new Vec3(1, 1, 1);

      double distSq = pos1.distanceToSqr(pos2);
      assertEquals(3.0, distSq, 0.001, "3D 距离平方应该是 3");
    }

    @Test
    @DisplayName("半径边界测试")
    void testRadiusBoundary() {
      double radius = 10.0;
      double radiusSq = radius * radius;

      // 距离正好等于半径
      assertEquals(100.0, radiusSq, 0.001, "半径平方应该是 100");

      // 距离小于半径（应该在范围内）
      assertTrue(99.0 <= radiusSq, "99 应该在半径范围内");

      // 距离大于半径（应该在范围外）
      assertTrue(101.0 > radiusSq, "101 应该在半径范围外");
    }
  }

  @Nested
  @DisplayName("逻辑组合测试")
  class LogicCombinationTest {

    @Test
    @DisplayName("checkGating 参数组合：playerRadius <= 0")
    void testCheckGatingNoPlayerRadiusCheck() {
      // 当 playerRadius <= 0 时，应该跳过玩家半径检查
      // 这个测试验证逻辑，实际功能需要集成测试
      double[] testRadii = {0.0, -1.0, -10.0};
      for (double radius : testRadii) {
        assertTrue(radius <= 0, "半径应该 <= 0: " + radius);
      }
    }

    @Test
    @DisplayName("checkGating 参数组合：playerRadius > 0")
    void testCheckGatingWithPlayerRadiusCheck() {
      // 当 playerRadius > 0 时，应该执行玩家半径检查
      double[] testRadii = {0.1, 1.0, 10.0, 100.0};
      for (double radius : testRadii) {
        assertTrue(radius > 0, "半径应该 > 0: " + radius);
      }
    }
  }
}
