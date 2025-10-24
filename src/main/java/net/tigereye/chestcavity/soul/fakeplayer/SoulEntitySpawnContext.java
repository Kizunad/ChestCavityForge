package net.tigereye.chestcavity.soul.fakeplayer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;

/**
 * 描述灵魂实体生成时的额外上下文信息。
 *
 * <p>主要用于指定自定义坐标/朝向或附加数据，供实体工厂在生成后进一步调整。
 */
public record SoulEntitySpawnContext(
    Optional<PlayerPositionSnapshot> positionOverride,
    Optional<Vec3> fallbackPosition,
    Map<String, String> customData) {

  public static final SoulEntitySpawnContext EMPTY =
      new SoulEntitySpawnContext(Optional.empty(), Optional.empty(), Map.of());

  public SoulEntitySpawnContext {
    positionOverride = positionOverride == null ? Optional.empty() : positionOverride;
    fallbackPosition = fallbackPosition == null ? Optional.empty() : fallbackPosition;
    customData = customData == null ? Map.of() : Map.copyOf(customData);
  }

  public Optional<String> customData(String key) {
    Objects.requireNonNull(key, "key");
    return Optional.ofNullable(customData.get(key));
  }
}
