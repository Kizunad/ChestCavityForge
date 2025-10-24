package net.tigereye.chestcavity.guzhenren.util;

import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 小境界阶段与转数推进的最小规则： - 当 "蛊师当前修炼进度" 达到/超过 "蛊师所需修炼进度" 时：当前进度清零，{@code jieduan}+1。 - 当 {@code jieduan}
 * 达到 {@link #MAX_JIEDUAN_PER_TURN}（默认 5）时：{@code zhuanshu}+1，{@code jieduan} 清零。
 */
public final class CultivationHelper {

  private CultivationHelper() {}

  public static final int MAX_JIEDUAN_PER_TURN = 5;

  /** 为玩家推进修炼进度并根据需要处理阶段/转数晋升。 */
  public static void tickProgress(
      GuzhenrenResourceBridge.ResourceHandle handle, double progressDelta) {
    if (handle == null || progressDelta <= 0.0) return;

    double need = handle.read("gushi_xiulian_jindu").orElse(0.0);
    if (!(need > 0.0)) return;

    // 增加当前进度并 clamp 到需求上限
    handle.adjustDouble("gushi_xiulian_dangqian", progressDelta, true, "gushi_xiulian_jindu");

    double cur = handle.read("gushi_xiulian_dangqian").orElse(0.0);
    if (cur + 1e-6 < need) {
      return; // 尚未达标
    }

    // 达标：清零当前进度，阶段+1
    handle.writeDouble("gushi_xiulian_dangqian", 0.0);
    handle.adjustDouble("jieduan", 1.0, true);

    // 检查是否进位为转数
    double jieduan = handle.read("jieduan").orElse(0.0);
    if (jieduan + 1e-6 >= MAX_JIEDUAN_PER_TURN) {
      handle.adjustDouble("zhuanshu", 1.0, true);
      handle.writeDouble("jieduan", 0.0);
    }
  }
}
