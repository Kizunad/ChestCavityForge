package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

/**
 * 上向模式（Up Vector Mode）。
 *
 * <p>WORLD_Y（默认）：使用世界 Y 轴作为上向基准；
 *
 * <p>OWNER_UP：使用主人朝向作为上向基准（未来扩展）。
 */
public enum UpMode {
  /** 世界 Y 轴作为上向（默认） */
  WORLD_Y,

  /** 主人朝向作为上向（预留扩展） */
  OWNER_UP
}
