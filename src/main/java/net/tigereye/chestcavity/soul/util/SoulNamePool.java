package net.tigereye.chestcavity.soul.util;

import java.util.Random;

/** Name pool for random SoulPlayer identities. You can edit RANDOM_NAMES to your liking. */
public final class SoulNamePool {

  private SoulNamePool() {}

  // 默认随机名池（可直接在此处增删，建议每项<=16字符）
  public static final String[] RANDOM_NAMES =
      new String[] {
        // 元素/五行/自然意象
        "玄魂",
        "青魂",
        "赤魂",
        "白魂",
        "黑魂",
        "金魂",
        "木魂",
        "水魂",
        "火魂",
        "土魂",
        "风魂",
        "雷魂",
        "云魂",
        "霜魂",
        "雪魂",
        "雾魂",
        "炎魂",
        "岚魂",
        // 抽象/状态
        "影",
        "灵",
        "魄",
        "游魂",
        "野魂",
        "幽灵",
        "夜游",
        "无常",
        // 典故/中二风
        "修罗",
        "冥行",
        "鬼行",
        "黑影",
        "星魂",
        "月魂",
        "日魂",
        "晨魂",
        "暮魂",
        // 计数/通用
        "魂一",
        "魂二",
        "魂三",
        "魂四",
        "魂五",
        "魂六",
        // 彩蛋（保持<=16字）
        "古月方源",
        "古月方正",
        "梦求真",
        "何春秋",
        "战部渡",
        "气海老祖",
        "房睇长"
      };

  public static String pick(Random random) {
    if (RANDOM_NAMES == null || RANDOM_NAMES.length == 0) return null;
    int idx = Math.max(0, random.nextInt(RANDOM_NAMES.length));
    return RANDOM_NAMES[idx];
  }
}
