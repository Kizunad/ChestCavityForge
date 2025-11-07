package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;

/**
 * TUI主题配置：统一的颜色、图标和视觉样式。
 *
 * <p>提供两套渲染方案：
 * <ul>
 *   <li>FANCY模式：使用emoji、Unicode边框和丰富颜色</li>
 *   <li>ASCII模式：使用纯文本字符，兼容旧客户端</li>
 * </ul>
 */
public final class TUITheme {

  private TUITheme() {}

  // ==================== 颜色主题 ====================

  public static final ChatFormatting ACCENT = ChatFormatting.GOLD; // 强调色
  public static final ChatFormatting BUTTON = ChatFormatting.AQUA; // 按钮颜色
  public static final ChatFormatting BUTTON_HOVER = ChatFormatting.DARK_AQUA; // 按钮悬停
  public static final ChatFormatting DIM = ChatFormatting.DARK_GRAY; // 暗淡文本
  public static final ChatFormatting TEXT = ChatFormatting.WHITE; // 正文
  public static final ChatFormatting LABEL = ChatFormatting.GRAY; // 标签
  public static final ChatFormatting VALUE = ChatFormatting.YELLOW; // 数值
  public static final ChatFormatting SUCCESS = ChatFormatting.GREEN; // 成功
  public static final ChatFormatting WARNING = ChatFormatting.YELLOW; // 警告
  public static final ChatFormatting ERROR = ChatFormatting.RED; // 错误

  // 模式颜色
  public static final ChatFormatting MODE_HUNT = ChatFormatting.RED; // 出击
  public static final ChatFormatting MODE_GUARD = ChatFormatting.BLUE; // 守护
  public static final ChatFormatting MODE_ORBIT = ChatFormatting.AQUA; // 环绕
  public static final ChatFormatting MODE_HOVER = ChatFormatting.GRAY; // 悬浮
  public static final ChatFormatting MODE_RECALL = ChatFormatting.DARK_GRAY; // 召回
  public static final ChatFormatting MODE_SWARM = ChatFormatting.GREEN; // 集群

  // ==================== Emoji 图标 ====================

  // 核心装饰
  public static final String EMOJI_SPARK = "✦"; // 装饰火花
  public static final String EMOJI_SEPARATOR = "·"; // 分隔符
  public static final String EMOJI_ARROW_LEFT = "◀"; // 左箭头
  public static final String EMOJI_ARROW_RIGHT = "▶"; // 右箭头
  public static final String EMOJI_CHECK = "✓"; // 勾选
  public static final String EMOJI_CROSS = "✗"; // 叉号
  public static final String EMOJI_CLOCK = "⏱"; // 时钟
  public static final String EMOJI_WARNING = "⚠"; // 警告

  // 模式图标
  public static final String EMOJI_HUNT = "⚔"; // 出击
  public static final String EMOJI_GUARD = "🛡"; // 守护
  public static final String EMOJI_ORBIT = "🌀"; // 环绕
  public static final String EMOJI_HOVER = "⏸"; // 悬浮
  public static final String EMOJI_RECALL = "🔁"; // 召回
  public static final String EMOJI_SWARM = "🌿"; // 集群

  // 功能图标
  public static final String EMOJI_SWORD = "🗡"; // 飞剑
  public static final String EMOJI_STORAGE = "📦"; // 存储
  public static final String EMOJI_REPAIR = "🔧"; // 修复
  public static final String EMOJI_GROUP = "👥"; // 分组
  public static final String EMOJI_TACTIC = "🎯"; // 战术

  // ==================== 布局参数 ====================

  private static final int MIN_FANCY_FRAME_WIDTH = 34; // 任意内容至少保持宽度
  private static final int MIN_ASCII_FRAME_WIDTH = 28;

  private static int lastFancyFrameWidth = MIN_FANCY_FRAME_WIDTH;
  private static int lastAsciiFrameWidth = MIN_ASCII_FRAME_WIDTH;

  // ==================== 边框样式 ====================

  /**
   * 创建顶部边框。
   *
   * @param title 标题文本
   * @return 格式化的边框组件
   */
  public static Component createTopBorder(String title) {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      String plain = EMOJI_SPARK + " " + title + " " + EMOJI_SPARK;
      int contentLen = visualLength(plain);
      int interior = Math.max(MIN_FANCY_FRAME_WIDTH, contentLen + 2);
      lastFancyFrameWidth = interior;
      int padding = Math.max(0, interior - contentLen);
      int leftPad = padding / 2;
      int rightPad = padding - leftPad;

      MutableComponent line = Component.literal("╭").withStyle(DIM);
      line.append(Component.literal(repeat('─', leftPad)).withStyle(DIM));
      line.append(Component.literal(EMOJI_SPARK + " ").withStyle(ACCENT));
      line.append(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      line.append(Component.literal(" " + EMOJI_SPARK).withStyle(ACCENT));
      line.append(Component.literal(repeat('─', rightPad)).withStyle(DIM));
      line.append(Component.literal("╮").withStyle(DIM));
      return line;
    } else {
      String plain = " " + title + " ";
      int contentLen = visualLength(plain);
      int interior = Math.max(MIN_ASCII_FRAME_WIDTH, contentLen + 2);
      lastAsciiFrameWidth = interior;
      int padding = Math.max(0, interior - contentLen);
      int leftPad = padding / 2;
      int rightPad = padding - leftPad;

      MutableComponent line = Component.literal("=").withStyle(DIM);
      line.append(Component.literal(repeat('=', leftPad)).withStyle(DIM));
      line.append(Component.literal(plain).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      line.append(Component.literal(repeat('=', rightPad)).withStyle(DIM));
      line.append(Component.literal("=").withStyle(DIM));
      return line;
    }
  }

  /**
   * 创建底部边框。
   *
   * @return 格式化的边框组件
   */
  public static Component createBottomBorder() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return Component.literal("╰")
          .withStyle(DIM)
          .append(Component.literal(repeat('─', lastFancyFrameWidth)).withStyle(DIM))
          .append(Component.literal("╯").withStyle(DIM));
    } else {
      return Component.literal(repeat('=', lastAsciiFrameWidth + 2)).withStyle(DIM);
    }
  }

  /**
   * 创建分隔线。
   *
   * @return 格式化的分隔线组件
   */
  public static Component createDivider() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return Component.literal("├")
          .withStyle(DIM)
          .append(Component.literal(repeat('─', lastFancyFrameWidth)).withStyle(DIM))
          .append(Component.literal("┤").withStyle(DIM));
    } else {
      return Component.literal(repeat('-', lastAsciiFrameWidth + 2)).withStyle(DIM);
    }
  }

  /**
   * 创建节标题（带图标）。
   *
   * @param icon 图标emoji
   * @param title 标题文本
   * @return 格式化的节标题组件
   */
  public static Component createSectionTitle(String icon, String title) {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return Component.literal("│ ")
          .withStyle(DIM)
          .append(Component.literal(icon + " ").withStyle(ACCENT))
          .append(Component.literal(title).withStyle(TEXT));
    } else {
      return Component.literal("▸ ").withStyle(ACCENT)
          .append(Component.literal(title).withStyle(TEXT));
    }
  }

  /**
   * 创建模式药丸（彩色标签）。
   *
   * @param mode 模式名称
   * @return 格式化的模式组件
   */
  public static Component createModePill(String mode) {
    String emoji;
    ChatFormatting color;

    switch (mode.toLowerCase()) {
      case "hunt", "出击" -> {
        emoji = EMOJI_HUNT;
        color = MODE_HUNT;
      }
      case "guard", "守护" -> {
        emoji = EMOJI_GUARD;
        color = MODE_GUARD;
      }
      case "orbit", "环绕" -> {
        emoji = EMOJI_ORBIT;
        color = MODE_ORBIT;
      }
      case "hover", "悬浮" -> {
        emoji = EMOJI_HOVER;
        color = MODE_HOVER;
      }
      case "recall", "召回" -> {
        emoji = EMOJI_RECALL;
        color = MODE_RECALL;
      }
      case "swarm", "集群" -> {
        emoji = EMOJI_SWARM;
        color = MODE_SWARM;
      }
      default -> {
        emoji = "?";
        color = LABEL;
      }
    }

    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return Component.literal(emoji + " " + mode).withStyle(color);
    } else {
      return Component.literal("[" + mode + "]").withStyle(color);
    }
  }

  /**
   * 创建标签：值格式的文本。
   *
   * @param label 标签
   * @param value 值
   * @return 格式化的组件
   */
  public static Component createLabelValue(String label, String value) {
    return Component.literal(label + ": ").withStyle(LABEL)
        .append(Component.literal(value).withStyle(VALUE));
  }

  /**
   * 创建标签：值格式的文本（带颜色）。
   *
   * @param label 标签
   * @param value 值
   * @param valueColor 值的颜色
   * @return 格式化的组件
   */
  public static Component createLabelValue(String label, String value, ChatFormatting valueColor) {
    return Component.literal(label + ": ").withStyle(LABEL)
        .append(Component.literal(value).withStyle(valueColor));
  }

  /**
   * 创建进度条。
   *
   * @param current 当前值
   * @param max 最大值
   * @param width 进度条宽度（字符数）
   * @param fullColor 已填充部分颜色
   * @param emptyColor 空白部分颜色
   * @return 格式化的进度条组件
   */
  public static Component createProgressBar(
      double current, double max, int width, ChatFormatting fullColor, ChatFormatting emptyColor) {
    double ratio = Math.min(1.0, Math.max(0.0, current / max));
    int filled = (int) Math.round(ratio * width);
    int empty = width - filled;

    String fullChar = FlyingSwordTuning.TUI_FANCY_EMOJI ? "█" : "#";
    String emptyChar = FlyingSwordTuning.TUI_FANCY_EMOJI ? "░" : "-";

    MutableComponent bar = Component.literal("");

    if (filled > 0) {
      bar.append(Component.literal(fullChar.repeat(filled)).withStyle(fullColor));
    }
    if (empty > 0) {
      bar.append(Component.literal(emptyChar.repeat(empty)).withStyle(emptyColor));
    }

    return bar;
  }

  /**
   * 创建按钮（可点击文本）。
   *
   * @param label 按钮文本
   * @return 格式化的按钮组件（不含命令和悬停）
   */
  public static MutableComponent createButton(String label) {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return Component.literal("[" + label + "]")
          .withStyle(BUTTON)
          .withStyle(ChatFormatting.UNDERLINE);
    } else {
      return Component.literal("[" + label + "]")
          .withStyle(BUTTON);
    }
  }

  /**
   * 创建导航按钮行。
   *
   * @param hasPrev 是否有上一页
   * @param hasNext 是否有下一页
   * @param currentPage 当前页码
   * @param totalPages 总页数
   * @return 格式化的导航栏组件
   */
  public static Component createNavigation(
      boolean hasPrev, boolean hasNext, int currentPage, int totalPages) {
    MutableComponent nav = Component.literal("");

    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      nav.append(Component.literal("├─ ").withStyle(DIM));

      if (hasPrev) {
        nav.append(Component.literal(EMOJI_ARROW_LEFT + " ").withStyle(BUTTON));
      } else {
        nav.append(Component.literal(EMOJI_ARROW_LEFT + " ").withStyle(DIM));
      }

      nav.append(
          Component.literal(" 第" + currentPage + "/" + totalPages + "页 ")
              .withStyle(LABEL));

      if (hasNext) {
        nav.append(Component.literal(" " + EMOJI_ARROW_RIGHT).withStyle(BUTTON));
      } else {
        nav.append(Component.literal(" " + EMOJI_ARROW_RIGHT).withStyle(DIM));
      }

      nav.append(Component.literal(" ─┤").withStyle(DIM));
    } else {
      if (hasPrev) {
        nav.append(Component.literal("< ").withStyle(BUTTON));
      } else {
        nav.append(Component.literal("< ").withStyle(DIM));
      }

      nav.append(
          Component.literal(" 第" + currentPage + "/" + totalPages + "页 ")
              .withStyle(LABEL));

      if (hasNext) {
        nav.append(Component.literal(" >").withStyle(BUTTON));
      } else {
        nav.append(Component.literal(" >").withStyle(DIM));
      }
    }

    return nav;
  }

  /**
   * 创建间隔符。
   *
   * @return 格式化的间隔符
   */
  public static Component createSpacer() {
    return Component.literal(" " + EMOJI_SEPARATOR + " ").withStyle(DIM);
  }

  private static String repeat(char ch, int count) {
    if (count <= 0) {
      return "";
    }
    return String.valueOf(ch).repeat(count);
  }

  private static int visualLength(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return text.codePointCount(0, text.length());
  }
}
