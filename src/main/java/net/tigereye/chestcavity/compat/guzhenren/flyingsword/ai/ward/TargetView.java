package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 仅用于拦截规划计算的目标视图抽象。
 *
 * <p>目的：在不引入 Minecraft 运行环境依赖的前提下，允许测试代码
 * 以纯几何数据提供目标信息，避免对 {@code Player} 等类进行 Mock。
 *
 * <p>KISS/YAGNI：只暴露算法所需的最小接口。
 */
public interface TargetView {
  Vec3 position();

  AABB getBoundingBox();

  double getBbHeight();
}

