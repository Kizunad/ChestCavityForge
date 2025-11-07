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

  // ==================== 框宽管理 ====================

  /** 当前页面的框宽（可视字符单位）。 */
  private static int currentFrameWidth = 50;

  /**
   * 开始新的框架，设置本页统一框宽。
   *
   * @param width 框架宽度（可视字符单位）
   */
  public static void beginFrame(int width) {
    currentFrameWidth = Math.max(20, width); // 最小宽度 20
  }

  /**
   * 获取当前框宽。
   *
   * @return 当前框宽
   */
  public static int getFrameWidth() {
    return currentFrameWidth;
  }

  /**
   * 估算文本的可视宽度（近似处理）。
   * <ul>
   *   <li>CJK 字符：2 宽</li>
   *   <li>常用 emoji：2 宽</li>
   *   <li>其他 ASCII：1 宽</li>
   * </ul>
   *
   * @param text 文本
   * @return 估算的可视宽度
   */
  public static int estimateVisualWidth(String text) {
    if (text == null) return 0;
    int width = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int cp = text.codePointAt(i);

      // 跳过高位代理对的第二个字符
      if (Character.isHighSurrogate(c)) {
        i++;
      }

      // CJK 统一表意文字
      if ((cp >= 0x4E00 && cp <= 0x9FFF) ||   // CJK Unified Ideographs
          (cp >= 0x3400 && cp <= 0x4DBF) ||   // CJK Extension A
          (cp >= 0x20000 && cp <= 0x2A6DF) || // CJK Extension B
          (cp >= 0x2A700 && cp <= 0x2B73F) || // CJK Extension C
          (cp >= 0x2B740 && cp <= 0x2B81F) || // CJK Extension D
          (cp >= 0x2B820 && cp <= 0x2CEAF) || // CJK Extension E
          (cp >= 0xF900 && cp <= 0xFAFF) ||   // CJK Compatibility Ideographs
          (cp >= 0x2F800 && cp <= 0x2FA1F)) { // CJK Compatibility Ideographs Supplement
        width += 2;
      }
      // 全角标点和符号
      else if ((cp >= 0x3000 && cp <= 0x303F) ||  // CJK Symbols and Punctuation
               (cp >= 0xFF00 && cp <= 0xFFEF)) {  // Halfwidth and Fullwidth Forms
        width += 2;
      }
      // 常用 emoji（简化判断）
      else if (cp >= 0x1F300 && cp <= 0x1F9FF) { // Emoticons, Symbols, Pictographs
        width += 2;
      }
      // 特殊 emoji 和符号（本项目常用）
      else if ("✦⚔🛡🌀⏸🔁🌿🗡📦🔧👥🎯◀▶✓✗⏱⚠·".indexOf(cp) >= 0) {
        width += 2;
      }
      // 其他字符按 1 宽
      else {
        width += 1;
      }
    }
    return width;
  }

  /**
   * 估算 Component 的可视宽度（仅纯文本，不考虑样式）。
   *
   * @param component 组件
   * @return 估算的可视宽度
   */
  public static int estimateVisualWidth(Component component) {
    if (component == null) return 0;
    String text = component.getString();
    return estimateVisualWidth(text);
  }

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

  // ==================== 边框样式 ====================

  /**
   * 创建顶部边框。
   *
   * @param title 标题文本
   * @return 格式化的边框组件
   */
  public static Component createTopBorder(String title) {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // 计算标题部分宽度："╭ " + "✦ " + title + " ✦" + " ╮"
      int titleVisualWidth = estimateVisualWidth(EMOJI_SPARK + " " + title + " " + EMOJI_SPARK);
      int borderWidth = 4; // "╭ " 和 " ╮"
      int totalUsed = borderWidth + titleVisualWidth;

      // 填充横线使总宽度等于 currentFrameWidth
      int fillNeeded = Math.max(0, currentFrameWidth - totalUsed);
      String fill = "─".repeat(fillNeeded / 2);

      return Component.literal("╭" + fill + " ")
          .withStyle(DIM)
          .append(Component.literal(EMOJI_SPARK + " ").withStyle(ACCENT))
          .append(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(TEXT))
          .append(Component.literal(" " + EMOJI_SPARK).withStyle(ACCENT))
          .append(Component.literal(" " + fill + "╮").withStyle(DIM));
    } else {
      int titleWidth = title.length();
      int fillNeeded = Math.max(5, (currentFrameWidth - titleWidth - 2) / 2);
      String fill = "=".repeat(fillNeeded);

      return Component.literal(fill + " ")
          .withStyle(DIM)
          .append(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(TEXT))
          .append(Component.literal(" " + fill).withStyle(DIM));
    }
  }

  /**
   * 创建底部边框。
   *
   * @return 格式化的边框组件
   */
  public static Component createBottomBorder() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // "╰" + 横线 + "╯"
      int fillNeeded = Math.max(0, currentFrameWidth - 2);
      String fill = "─".repeat(fillNeeded);
      return Component.literal("╰" + fill + "╯").withStyle(DIM);
    } else {
      String fill = "=".repeat(Math.max(0, currentFrameWidth));
      return Component.literal(fill).withStyle(DIM);
    }
  }

  /**
   * 创建分隔线。
   *
   * @return 格式化的分隔线组件
   */
  public static Component createDivider() {
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      // "├" + 虚线 + "┤"
      int fillNeeded = Math.max(0, currentFrameWidth - 2);
      // 使用交替的 "─ " 模式创建虚线效果
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fillNeeded; i++) {
        sb.append(i % 2 == 0 ? "─" : " ");
      }
      return Component.literal("├" + sb + "┤").withStyle(DIM);
    } else {
      String fill = "─".repeat(Math.max(0, currentFrameWidth));
      return Component.literal(fill).withStyle(DIM);
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

    // 计算内容可视宽度
    int contentWidth = estimateVisualWidth(content);
    // 左边框 "│ " 宽 2，右边框 " │" 宽 2
    int borderWidth = 4;
    int usedWidth = contentWidth + borderWidth;

    // 计算需要填充的宽度
    int fillNeeded = Math.max(0, currentFrameWidth - usedWidth);
    String fill = " ".repeat(fillNeeded);

    return Component.literal("│ ")
        .withStyle(DIM)
        .append(content)
        .append(Component.literal(fill + " │").withStyle(DIM));
  }

  /**
   * 包装内容行（无样式版本，用于已经有左边框的内容）。
   * <p>仅在右侧添加填充和右边框。
   *
   * @param content 已包含左边框的内容
   * @return 包装后的组件
   */
  public static Component wrapContentLineRaw(Component content) {
    if (!FlyingSwordTuning.TUI_FANCY_EMOJI) {
      return content;
    }

    int contentWidth = estimateVisualWidth(content);
    int fillNeeded = Math.max(0, currentFrameWidth - contentWidth - 2); // 减去右边框 " │"
    String fill = " ".repeat(fillNeeded);

    return Component.empty()
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
    Component content;
    if (FlyingSwordTuning.TUI_FANCY_EMOJI) {
      content = Component.literal(icon + " ").withStyle(ACCENT)
          .append(Component.literal(title).withStyle(TEXT));
      return wrapContentLine(content);
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
}
