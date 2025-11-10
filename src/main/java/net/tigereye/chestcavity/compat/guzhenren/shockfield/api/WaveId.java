package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

import java.util.Objects;
import java.util.UUID;

/**
 * 波源标识：用于区分不同的冲击波源，避免同帧自伤和友伤。
 *
 * <p>每个 WaveId 包含：
 *
 * <ul>
 *   <li>sourceId: 波源所有者的UUID
 *   <li>spawnTick: 波源生成的游戏tick
 *   <li>serial: 同一tick内的序列号（用于区分多个波源）
 * </ul>
 */
public record WaveId(UUID sourceId, long spawnTick, int serial) {

  public WaveId {
    Objects.requireNonNull(sourceId, "sourceId");
  }

  public static WaveId of(UUID sourceId, long spawnTick, int serial) {
    return new WaveId(sourceId, spawnTick, serial);
  }

  public static WaveId of(UUID sourceId, long spawnTick) {
    return new WaveId(sourceId, spawnTick, 0);
  }

  @Override
  public String toString() {
    return String.format(
        "WaveId[%s@%d#%d]", sourceId.toString().substring(0, 8), spawnTick, serial);
  }
}
