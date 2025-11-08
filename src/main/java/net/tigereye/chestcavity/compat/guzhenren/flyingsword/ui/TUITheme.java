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

  private static final int MIN_FANCY_FRAME_WIDTH = 20;
  private static final int MIN_ASCII_FRAME_WIDTH = 20;

  private static int fancyFrameWidth = MIN_FANCY_FRAME_WIDTH;
  private static int asciiFrameWidth = MIN_ASCII_FRAME_WIDTH;
  private static int borderWidth = FlyingSwordTuning.TUI_BORDER_WIDTH;

  /** 设置当前界面的统一框宽度（可视字符单位），并刷新边框宽度。 */
  public static void beginFrame(int desiredWidth) {
    int maxWidth = Math.max(MIN_FANCY_FRAME_WIDTH, FlyingSwordTuning.TUI_FRAME_MAX_WIDTH);
    fancyFrameWidth = Math.min(maxWidth, Math.max(MIN_FANCY_FRAME_WIDTH, desiredWidth));
    asciiFrameWidth = Math.min(maxWidth, Math.max(MIN_ASCII_FRAME_WIDTH, desiredWidth));
    int borderMin = Math.max(16, FlyingSwordTuning.TUI_BORDER_WIDTH);
    borderWidth = Math.max(borderMin, fancyFrameWidth);
  }

  private static int currentBorderWidth() {
    return borderWidth;
  }

  // ==================== 公共度量（统计字数/可视宽度） ====================

  /** 统计字符串在TUI中的可视宽度（使用精确的全角/半角计算）。 */
  public static int measureWidth(String text) {
    return CharWidthCalculator.calculateWidth(text);
  }

  /** 统计组件在TUI中的可视宽度。 */
  public static int measureWidth(Component component) {
    if (component == null) return 0;
    // getString() 已经会递归获取所有子组件的文本
    return CharWidthCalculator.calculateWidth(component.getString());
  }

  /**
   * 估算若干文本的最大宽度，并返回一个适合作为 frame 宽度的值（加上适度余量）。
   */
  public static int estimateFrameWidthFromStrings(int minWidth, String... lines) {
    int max = 0;
    if (lines != null) {
      for (String s : lines) {
        if (s == null) continue;
        max = Math.max(max, CharWidthCalculator.calculateWidth(s));
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
        max = Math.max(max, CharWidthCalculator.calculateWidth(c.getString()));
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
      int interior = Math.max(0, currentBorderWidth() - 2);
      String content = title;
      int contentWidth = codePointLength(content);
      int padding = Math.max(0, interior - contentWidth);
      int left = padding / 2;
      int right = padding - left;

      MutableComponent line = Component.literal("╭").withStyle(DIM);
      if (left > 0) {
        line.append(Component.literal(
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                ? repeat('─', left)
                : padUnits(left)).withStyle(DIM));
      }
      line.append(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      if (right > 0) {
        line.append(Component.literal(
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                ? repeat('─', right)
                : padUnits(right)).withStyle(DIM));
      }
      line.append(Component.literal("╮").withStyle(DIM));
      return line;
    } else {
      int interior = Math.max(0, currentBorderWidth() - 2);
      String content = " " + title + " ";
      int contentWidth = codePointLength(content);
      int padding = Math.max(0, interior - contentWidth);
      int left = padding / 2;
      int right = padding - left;

      MutableComponent line = Component.literal("=").withStyle(DIM);
      if (left > 0) {
        line.append(Component.literal(
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                ? repeat('=', left)
                : padUnits(left)).withStyle(DIM));
      }
      line.append(Component.literal(content).withStyle(ChatFormatting.BOLD).withStyle(TEXT));
      if (right > 0) {
        line.append(Component.literal(
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                ? repeat('=', right)
                : padUnits(right)).withStyle(DIM));
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
      // 与独立边框宽度一致
      int interior = Math.max(0, currentBorderWidth() - 2);
      return Component.literal("╰" + (
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                  ? repeat('─', interior)
                  : padUnits(interior)) + "╯").withStyle(DIM);
    } else {
      // ASCII 模式宽度 = 边框宽度
      int width = currentBorderWidth();
      return Component.literal(
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                  ? repeat('=', width)
                  : padUnits(width))
          .withStyle(DIM);
    }
  }

  /**
   * 创建分隔线。
   *
   * @return 格式化的分隔线组件
   */
  public static Component createDivider() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // 使用边框宽度，确保左右边界对齐
      int interior = Math.max(0, currentBorderWidth() - 2);
      return Component.literal("├" + (
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                  ? repeat('─', interior)
                  : padUnits(interior)) + "┤").withStyle(DIM);
    } else {
      // ASCII 模式同样对齐边框宽度
      int width = currentBorderWidth();
      return Component.literal(
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.TUI_VISIBLE_HLINES
                  ? repeat('-', width)
                  : padUnits(width))
          .withStyle(DIM);
    }
  }

  /**
   * 包装内容行（无左右边框，仅对齐）。
   *
   * @param content 内容组件
   * @return 包装后的组件
   */
  public static Component wrapContentLine(Component content) {
    // 直接返回内容，不添加边框
    return content == null ? Component.literal("") : content;
  }

  // ==================== 插入/对齐辅助 ====================

  /** 对齐方式。 */
  public enum Align { LEFT, CENTER, RIGHT }

  /**
   * 按对齐方式包装一行内容（无边框，仅对齐）。
   */
  public static Component wrapContentLineAligned(Component content, Align align) {
    // 无边框模式：直接返回内容
    return content == null ? Component.literal("") : content;
  }

  // ==================== 内容行（无边框） ====================

  /** 创建一个空白行。 */
  public static Component createEmptyLine() {
    return Component.literal("");
  }

  /**
   * 创建内容行（无左右边框，保留所有样式）。
   */
  public static Component createContentLine(Component content) {
    return content == null ? Component.literal("") : content;
  }


  /** 将字符串按可视宽度裁剪到不超过 maxWidth（考虑 CJK/emoji 宽度）。 */
  public static String truncateToVisualWidth(String s, int maxWidth) {
    return CharWidthCalculator.truncate(s, maxWidth);
  }


  private static String repeat(char ch, int count) {
    if (count <= 0) {
      return "";
    }
    return String.valueOf(ch).repeat(count);
  }

  /** 以"单位"为计数的填充：当启用全角模式时，用 U+3000；否则用普通空格或降级字符。 */
  private static String padUnits(int units) {
    if (units <= 0) return "";
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
        .FlyingSwordTuning.TUI_FULLWIDTH_PAD) {
      // 全角空格占2单位，所以需要除以2（向下取整）
      // 如果units是奇数，会少1单位，用半角空格补齐
      int fullwidthCount = units / 2;
      int remainder = units % 2;
      String result = "　".repeat(fullwidthCount);
      if (remainder > 0) {
        result += " ";  // 补充半角空格
      }
      return result;
    }
    // 默认使用半角空格
    return " ".repeat(units);
  }

  // 注：宽度计算已迁移到 CharWidthCalculator
  // 近似等宽度量：仅按 codePoint 计数，用于顶部/ASCII边框的居中对齐。
  private static int codePointLength(String text) {
    if (text == null || text.isEmpty()) return 0;
    return text.codePointCount(0, text.length());
  }
}
