package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

/**
 * Minecraft聊天系统中的字符宽度计算器。
 *
 * <p>Minecraft使用等宽字体，但中文、英文、emoji的宽度不同：
 * <ul>
 *   <li>英文字母/数字/半角符号：1 个单位（~5px）</li>
 *   <li>中文字符/全角符号/emoji：2 个单位（~10px）</li>
 *   <li>空格：根据上下文，半角空格1单位，全角空格2单位</li>
 * </ul>
 */
public final class CharWidthCalculator {
  private CharWidthCalculator() {}

  /**
   * 计算字符串的实际显示宽度（以"单位"为基准，1单位≈5px）。
   *
   * @param text 输入文本
   * @return 宽度单位数
   */
  public static int calculateWidth(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }

    int width = 0;
    for (int i = 0; i < text.length(); ) {
      int codePoint = text.codePointAt(i);
      width += getCharWidth(codePoint);
      i += Character.charCount(codePoint);
    }
    return width;
  }

  /**
   * 获取单个字符的宽度。
   *
   * @param codePoint Unicode代码点
   * @return 宽度单位（1 = 半角，2 = 全角）
   */
  public static int getCharWidth(int codePoint) {
    // 特殊处理常见的半角字符
    if (codePoint >= 0x21 && codePoint <= 0x7E) {
      // ASCII可打印字符（!, ", #, ..., ~）
      if (codePoint >= 0x30 && codePoint <= 0x39) {
        // 数字 0-9：更窄，约4-5px
        return 1;
      }
      if (codePoint >= 0x41 && codePoint <= 0x5A) {
        // 大写字母 A-Z：约5px
        return 1;
      }
      if (codePoint >= 0x61 && codePoint <= 0x7A) {
        // 小写字母 a-z：约4px
        return 1;
      }
      // 其他ASCII符号
      return 1;
    }

    // 空格处理
    if (codePoint == 0x20) {
      // 半角空格：4px
      return 1;
    }
    if (codePoint == 0x3000) {
      // 全角空格：10px
      return 2;
    }

    // CJK 统一表意文字及扩展（中日韩）
    if ((codePoint >= 0x4E00 && codePoint <= 0x9FFF)      // CJK 基础区
        || (codePoint >= 0x3400 && codePoint <= 0x4DBF)   // CJK 扩展A
        || (codePoint >= 0x20000 && codePoint <= 0x2A6DF) // CJK 扩展B
        || (codePoint >= 0x2A700 && codePoint <= 0x2B73F) // CJK 扩展C
        || (codePoint >= 0x2B740 && codePoint <= 0x2B81F) // CJK 扩展D
        || (codePoint >= 0x2B820 && codePoint <= 0x2CEAF) // CJK 扩展E
        || (codePoint >= 0xF900 && codePoint <= 0xFAFF)   // CJK 兼容表意字
        || (codePoint >= 0x2F800 && codePoint <= 0x2FA1F)) { // CJK 兼容表意扩展
      return 2;
    }

    // CJK 标点/全角符号
    if ((codePoint >= 0x3000 && codePoint <= 0x303F)      // CJK 符号和标点
        || (codePoint >= 0xFF00 && codePoint <= 0xFFEF)) { // 全角和半角形式
      return 2;
    }

    // 杂项技术符号 (包括 ⏸ ⏱ 等)
    if (codePoint >= 0x2300 && codePoint <= 0x23FF) {
      return 2;
    }

    // 杂项符号 (包括 ⚔ ⚠ 等)
    if (codePoint >= 0x2600 && codePoint <= 0x26FF) {
      return 2;
    }

    // 装饰符号 (包括 ✦ ✓ ✗ 等)
    if (codePoint >= 0x2700 && codePoint <= 0x27BF) {
      return 2;
    }

    // 箭头符号 (包括 ◀ ▶ 等)
    if (codePoint >= 0x25A0 && codePoint <= 0x25FF) {
      return 2;
    }

    // 主要 emoji 区段
    if (codePoint >= 0x1F300 && codePoint <= 0x1FAD6) {
      return 2;
    }

    // 补充符号和象形文字
    if (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) {
      return 2;
    }

    // 其他Unicode字符（保守估计为1）
    return 1;
  }

  /**
   * 将字符串截断到指定宽度。
   *
   * @param text 输入文本
   * @param maxWidth 最大宽度单位
   * @return 截断后的字符串
   */
  public static String truncate(String text, int maxWidth) {
    if (text == null || text.isEmpty() || maxWidth <= 0) {
      return "";
    }

    int width = 0;
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < text.length(); ) {
      int codePoint = text.codePointAt(i);
      int charWidth = getCharWidth(codePoint);
      if (width + charWidth > maxWidth) {
        break;
      }
      result.appendCodePoint(codePoint);
      width += charWidth;
      i += Character.charCount(codePoint);
    }
    return result.toString();
  }

  /**
   * 用空格补充到指定宽度（右对齐）。
   *
   * @param text 输入文本
   * @param targetWidth 目标宽度
   * @return 补充后的字符串
   */
  public static String padRight(String text, int targetWidth) {
    int currentWidth = calculateWidth(text);
    int needed = targetWidth - currentWidth;
    if (needed <= 0) {
      return text;
    }
    return text + " ".repeat(needed);
  }

  /**
   * 用空格补充到指定宽度（左对齐）。
   */
  public static String padLeft(String text, int targetWidth) {
    int currentWidth = calculateWidth(text);
    int needed = targetWidth - currentWidth;
    if (needed <= 0) {
      return text;
    }
    return " ".repeat(needed) + text;
  }

  /**
   * 用空格补充到指定宽度（居中）。
   */
  public static String padCenter(String text, int targetWidth) {
    int currentWidth = calculateWidth(text);
    int needed = targetWidth - currentWidth;
    if (needed <= 0) {
      return text;
    }
    int left = needed / 2;
    int right = needed - left;
    return " ".repeat(left) + text + " ".repeat(right);
  }
}
