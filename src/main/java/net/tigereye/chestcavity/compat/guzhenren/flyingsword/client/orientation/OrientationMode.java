package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation;

/**
 * 姿态计算模式（Orientation Calculation Mode）。
 *
 * <p>BASIS（默认）：基于正交基/四元数统一计算，避免欧拉角不对称；
 * <p>LEGACY_EULER：沿用旧有 Y→Z 欧拉顺序（兼容）。
 */
public enum OrientationMode {
  /** 正交基/四元数计算（默认，解决半圆抬头问题） */
  BASIS,

  /** 遗留欧拉角顺序（兼容旧资源） */
  LEGACY_EULER
}
