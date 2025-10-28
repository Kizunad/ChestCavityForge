package net.tigereye.chestcavity.compat.guzhenren.util;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import org.jetbrains.annotations.Nullable;

/**
 * 运行时工具：解析 ItemStack 的 tooltip，识别古真人物品的“流派”标识。
 *
 * <p>使用场景：ModernUI 配置界面或调试命令希望读取玩家当前指向物品的流派信息， 而无需维护额外数据表。
 */
public final class GuzhenrenFlowTooltipResolver {
  private static final Pattern FLOW_PATTERN = Pattern.compile("流派[:：]\\s*(.+)"); // 提取“流派：xxx”主干文本
  private static final Pattern FLOW_DELIMITER = Pattern.compile("[、，,\\s]+"); // 支持多流派分隔符

  private GuzhenrenFlowTooltipResolver() {}

  /**
   * 扫描 tooltip 行，返回物品 ID 与解析出的流派列表。
   *
   * @param stack 需要检查的物品
   * @param context tooltip 上下文（通常来自 {@link Item.TooltipContext#of(Level)} 或客户端提供）
   * @param flag tooltip 标志（一般为 {@link TooltipFlag#NORMAL}）
   * @param viewer 当前查看 tooltip 的玩家，可为空
   */
  public static FlowInfo inspect(
      ItemStack stack, TooltipContext context, TooltipFlag flag, @Nullable Player viewer) {
    List<net.minecraft.network.chat.Component> lines = stack.getTooltipLines(context, viewer, flag);
    return inspect(stack, lines);
  }

  /** 直接使用外部提供的 tooltip 文本（例如已经渲染的 UI 缓存）。 */
  public static FlowInfo inspect(
      ItemStack stack, List<net.minecraft.network.chat.Component> tooltipLines) {
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    Set<String> flows = new LinkedHashSet<>();

    for (net.minecraft.network.chat.Component component : tooltipLines) {
      String text = normalize(component.getString());
      if (text.isEmpty()) {
        continue;
      }
      var matcher = FLOW_PATTERN.matcher(text);
      if (!matcher.find()) {
        continue;
      }
      String value = matcher.group(1).trim();
      if (value.isEmpty()) {
        continue;
      }
      if (isAbsentMarker(value)) {
        flows.clear();
        break;
      }
      FLOW_DELIMITER
          .splitAsStream(value)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(flows::add);
    }

    return new FlowInfo(itemId, ImmutableList.copyOf(flows));
  }

  private static String normalize(String raw) {
    if (raw == null) {
      return "";
    }
    String stripped = ChatFormatting.stripFormatting(raw);
    String fallback = stripped == null ? raw : stripped;
    return fallback.trim();
  }

  /** tooltip 中若出现这些词视为“无流派”。 */
  private static boolean isAbsentMarker(String value) {
    String lowered = value.toLowerCase(Locale.ROOT);
    return lowered.isEmpty()
        || "无".equals(value)
        || "暂无".equals(value)
        || lowered.contains("无对应")
        || lowered.contains("未划分");
  }

  public record FlowInfo(@Nullable ResourceLocation itemId, ImmutableList<String> flows) {
    public boolean hasFlow() {
      return !flows.isEmpty();
    }
  }

  /**
   * 便捷方法：基于 Level 构建 {@link TooltipContext} 并解析流派。
   *
   * <p>注意：NeoForge 1.21 的 {@link TooltipContext} 需要通过 {@link Item.TooltipContext#of(Level)} 或
   * {@link Item.TooltipContext#none()} 创建，调用端需确保在客户端线程执行。
   */
  public static FlowInfo inspectWithLevel(
      ItemStack stack, @Nullable Level level, @Nullable Player viewer, TooltipFlag flag) {
    TooltipContext context = level != null ? TooltipContext.of(level) : TooltipContext.EMPTY;
    return inspect(stack, context, flag, viewer);
  }

  // ======== 整道系识别（方便在行为侧整体判断“是否具备 X 道器官”） ========

  /**
   * 判断玩家胸腔中是否存在任意物品的 tooltip 流派包含指定关键字（如“力道”“水道”“奴道”）。
   * 采用大小写不敏感匹配，并做“去除‘道’后缀”的宽松匹配。
   */
  public static boolean hasFlow(ChestCavityInstance cc, Level level, String flowKeyword) {
    if (cc == null || cc.inventory == null || level == null || flowKeyword == null) {
      return false;
    }
    String normalizedKey = normalizeFlowKeyword(flowKeyword);
    TooltipContext context = TooltipContext.of(level);
    TooltipFlag flag = TooltipFlag.NORMAL;
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      FlowInfo info = inspect(stack, context, flag, null);
      if (!info.hasFlow()) continue;
      for (String f : info.flows()) {
        if (flowMatch(normalizedKey, f)) {
          return true;
        }
      }
    }
    return false;
  }

  /** 任一关键字命中即返回 true。 */
  public static boolean hasAnyFlow(ChestCavityInstance cc, Level level, String... flowKeywords) {
    if (flowKeywords == null || flowKeywords.length == 0) return false;
    for (String k : flowKeywords) {
      if (hasFlow(cc, level, k)) return true;
    }
    return false;
  }

  private static boolean flowMatch(String normalizedKey, String rawFlow) {
    if (rawFlow == null) return false;
    String f = normalizeFlowKeyword(rawFlow);
    // 完整包含 或 去尾后匹配（例如 影/影道）
    return f.contains(normalizedKey)
        || stripDaoSuffix(f).contains(stripDaoSuffix(normalizedKey));
  }

  private static String normalizeFlowKeyword(String s) {
    if (s == null) return "";
    String lowered = s.toLowerCase(Locale.ROOT).trim();
    // 统一全角/空格等：这里只做最小处理
    return lowered;
  }

  private static String stripDaoSuffix(String s) {
    if (s.endsWith("道")) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }
}
