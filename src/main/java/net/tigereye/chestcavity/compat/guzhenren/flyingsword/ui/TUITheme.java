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

  private static final int MIN_FANCY_FRAME_WIDTH = 34;
  private static final int MIN_ASCII_FRAME_WIDTH = 28;

  private static int fancyFrameWidth = MIN_FANCY_FRAME_WIDTH;
  private static int asciiFrameWidth = MIN_ASCII_FRAME_WIDTH;

  /** 设置当前界面的统一框宽度（可视字符单位）。 */
  public static void beginFrame(int desiredWidth) {
    fancyFrameWidth = Math.max(MIN_FANCY_FRAME_WIDTH, desiredWidth);
    asciiFrameWidth = Math.max(MIN_ASCII_FRAME_WIDTH, desiredWidth);
  }

  private static int currentFrameWidth() {
    return FlyingSwordTuning.TUI_FANCY_EMOJI ? fancyFrameWidth : asciiFrameWidth;
  }

  /**
   * 估算若干文本的最大宽度，并返回一个适合作为 frame 宽度的值（加上适度余量）。
   */
  public static int estimateFrameWidthFromStrings(int minWidth, String... lines) {
    int max = 0;
    if (lines != null) {
      for (String s : lines) {
        if (s == null) continue;
        max = Math.max(max, visualLength(s));
      }
    }
    // 额外加 4 个字符余量（两侧留白）
    int desired = Math.max(minWidth, max + 4);
    return desired;
  }

  /**
   * 估算若干 Component 的最大宽度，并返回一个适合作为 frame 宽度的值（加上适度余量）。
   */
  public static int estimateFrameWidth(int minWidth, Component... lines) {
    int max = 0;
    if (lines != null) {
      for (Component c : lines) {
        if (c == null) continue;
        max = Math.max(max, visualLength(c.getString()));
      }
    }
    int desired = Math.max(minWidth, max + 4);
    return desired;
  }

  // ==================== 边框样式 ====================

  /**
   * 创建顶部边框。
   *
   * @param title 标题文本
   * @return 格式化的边框组件
   */
  public static Component createTopBorder(String title) {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // 限制内部最大宽度为60
      int interior = Math.max(0, Math.min(60, currentFrameWidth() - 2));
      String content = EMOJI_SPARK + " " + title + " " + EMOJI_SPARK;
      int contentWidth = visualLength(content);
      int padding = Math.max(0, interior - contentWidth);
      int left = padding / 2;
      int right = padding - left;

      MutableComponent line = Component.literal("╭").withStyle(DIM);
      if (left > 0) {
        line.append(Component.literal(repeat('─', left)).withStyle(DIM));
      }
      line.append(Component.literal(EMOJI_SPARK + " ").withStyle(ACCENT));
      line.append(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      line.append(Component.literal(" " + EMOJI_SPARK).withStyle(ACCENT));
      if (right > 0) {
        line.append(Component.literal(repeat('─', right)).withStyle(DIM));
      }
      line.append(Component.literal("╮").withStyle(DIM));
      return line;
    } else {
      // ASCII模式也限制宽度
      int interior = Math.max(0, Math.min(60, currentFrameWidth() - 2));
      String content = " " + title + " ";
      int contentWidth = visualLength(content);
      int padding = Math.max(0, interior - contentWidth);
      int left = padding / 2;
      int right = padding - left;

      MutableComponent line = Component.literal("=").withStyle(DIM);
      if (left > 0) {
        line.append(Component.literal(repeat('=', left)).withStyle(DIM));
      }
      line.append(Component.literal(content).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      if (right > 0) {
        line.append(Component.literal(repeat('=', right)).withStyle(DIM));
      }
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
      // 限制内部最大宽度为60
      int interior = Math.max(0, Math.min(60, currentFrameWidth() - 2));
      return Component.literal("╰" + repeat('─', interior) + "╯").withStyle(DIM);
    } else {
      // ASCII模式也限制宽度
      int width = Math.min(60, currentFrameWidth());
      return Component.literal(repeat('=', width)).withStyle(DIM);
    }
  }

  /**
   * 创建分隔线。
   *
   * @return 格式化的分隔线组件
   */
  public static Component createDivider() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // 计算适当的内部宽度，避免过长
      int interior = Math.max(0, Math.min(60, currentFrameWidth() - 2));
      return Component.literal("├" + repeat('─', interior) + "┤").withStyle(DIM);
    } else {
      // ASCII模式也限制最大宽度
      int width = Math.min(60, currentFrameWidth());
      return Component.literal(repeat('-', width)).withStyle(DIM);
    }
  }

  /**
   * 包装内容行，添加左右边框实现闭合效果。
   * <p>模式：左边框 + 内容 + 填充 + 右边框
   *
   * @param content 内容组件
   * @return 包装后的组件
   */
  public static Component wrapContentLine(Component content) {
    if (!FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // ASCII 模式不添加边框，直接返回内容
      return content;
    }

    int contentWidth = visualLength(content.getString());
    int borderWidth = 4; // 左右边框
    int fillNeeded = Math.max(0, currentFrameWidth() - contentWidth - borderWidth);
    String fill = fillNeeded > 0 ? repeat(' ', fillNeeded) : "";

    return Component.literal("│ ")
        .withStyle(DIM)
        .append(content)
        .append(Component.literal(fill + " │").withStyle(DIM));
  }

  /**
   * 创建节标题（带图标）。
   *
   * @param icon 图标emoji
   * @param title 标题文本
   * @return 格式化的节标题组件
   */
  public static Component createSectionTitle(String icon, String title) {
    MutableComponent content = Component.literal("");
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      content.append(Component.literal(icon + " ").withStyle(ACCENT));
      content.append(Component.literal(title).withStyle(TEXT));
    } else {
      content.append(Component.literal("▸ " + title).withStyle(TEXT));
    }
    return content;
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
    int width = 0;
    for (int i = 0; i < text.length(); ) {
      int cp = text.codePointAt(i);
      i += Character.charCount(cp);
      width += isWide(cp) ? 2 : 1;
    }
    return width;
  }

  private static boolean isWide(int cp) {
    // CJK 统一表意 + 扩展
    if ((cp >= 0x4E00 && cp <= 0x9FFF)
        || (cp >= 0x3400 && cp <= 0x4DBF)
        || (cp >= 0x20000 && cp <= 0x2A6DF)
        || (cp >= 0x2A700 && cp <= 0x2B73F)
        || (cp >= 0x2B740 && cp <= 0x2B81F)
        || (cp >= 0x2B820 && cp <= 0x2CEAF)
        || (cp >= 0xF900 && cp <= 0xFAFF)
        || (cp >= 0x2F800 && cp <= 0x2FA1F)) {
      return true;
    }
    // CJK 标点/全角符号
    if ((cp >= 0x3000 && cp <= 0x303F)
        || (cp >= 0xFF00 && cp <= 0xFFEF)) {
      return true;
    }
    // 常用 emoji 区段
    if (cp >= 0x1F300 && cp <= 0x1FAD6) {
      return true;
    }
    // 项目常用符号
    if ("✦⚔🛡🌀⏸🔁🌿🗡📦🔧👥🎯◀▶✓✗⏱⚠·".indexOf(cp) >= 0) {
      return true;
    }
    return false;
  }
}
