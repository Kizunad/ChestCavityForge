package net.tigereye.chestcavity.soul.fakeplayer;

import java.util.Optional;

/**
 * 抽象工厂：按请求生成或复原灵魂相关实体。
 */
@FunctionalInterface
public interface SoulEntityFactory {

    Optional<SoulEntitySpawnResult> spawn(SoulEntitySpawnRequest request);
}

