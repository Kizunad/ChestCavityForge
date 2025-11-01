package net.tigereye.chestcavity.compat.guzhenren.util;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.registry.GZRItemTags;
import org.jetbrains.annotations.Nullable;

/** 运行时工具：基于标签识别古真人物品所属的“流派”标识。 */
public final class GuzhenrenFlowTooltipResolver {
  private static final Map<String, TagKey<Item>> FLOW_TAGS = GZRItemTags.FLOW_TAGS;
  /** 归一“剑道”顶层标签：用于统一判定是否属于剑道相关物品/技能。 */
  private static final TagKey<Item> JIANDAO_TAG =
      TagKey.create(Registries.ITEM, ResourceLocation.parse("guzhenren:jiandao"));

  private GuzhenrenFlowTooltipResolver() {}

  /** 扫描物品标签，返回物品 ID 与解析出的流派列表。 */
  public static FlowInfo inspect(
      ItemStack stack, TooltipContext context, TooltipFlag flag, @Nullable Player viewer) {
    return inspect(stack);
  }

  /** 兼容旧签名：忽略 tooltip 文本，直接走标签识别。 */
  public static FlowInfo inspect(
      ItemStack stack, List<net.minecraft.network.chat.Component> tooltipLines) {
    return inspect(stack);
  }

  public static FlowInfo inspect(ItemStack stack) {
    if (stack.isEmpty()) {
      return new FlowInfo(null, ImmutableList.of());
    }
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return new FlowInfo(itemId, resolveFlows(stack));
  }

  private static ImmutableList<String> resolveFlows(ItemStack stack) {
    List<String> flows = new ArrayList<>();
    for (Map.Entry<String, TagKey<Item>> entry : FLOW_TAGS.entrySet()) {
      if (stack.is(entry.getValue())) {
        flows.add(entry.getKey());
      }
    }
    return flows.isEmpty() ? ImmutableList.of() : ImmutableList.copyOf(flows);
  }

  public record FlowInfo(@Nullable ResourceLocation itemId, ImmutableList<String> flows) {
    public boolean hasFlow() {
      return !flows.isEmpty();
    }
  }

  /** 兼容旧签名：忽略上下文参数，直接使用标签信息。 */
  public static FlowInfo inspectWithLevel(
      ItemStack stack, @Nullable Level level, @Nullable Player viewer, TooltipFlag flag) {
    return inspect(stack);
  }

  // ======== 整道系识别（方便在行为侧整体判断“是否具备 X 道器官”） ========

  /** 判断玩家胸腔中是否存在任意物品的标签流派包含指定关键字（如“力道”“水道”“奴道”）。 */
  public static boolean hasFlow(ChestCavityInstance cc, Level level, String flowKeyword) {
    if (cc == null || cc.inventory == null || level == null || flowKeyword == null) {
      return false;
    }
    String normalizedKey = normalizeFlowKeyword(flowKeyword);
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      FlowInfo info = inspect(stack);
      if (!info.hasFlow()) {
        continue;
      }
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

  private static FlowInfo emptyFlow(ItemStack stack) {
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return new FlowInfo(itemId, ImmutableList.of());
  }

  // ======== 剑道（Jiandao）检测辅助 ========

  /**
   * 判定物品是否属于“剑道”逻辑。
   *
   * <p>优先依据顶层标签 guzhenren:jiandao；若缺失则回退到流派关键字匹配（"jiandao"/"jian_dao"/"剑道"）。
   */
  public static boolean isJiandaoItem(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    if (stack.is(JIANDAO_TAG)) {
      return true;
    }
    FlowInfo info = inspect(stack);
    if (!info.hasFlow()) {
      return false;
    }
    for (String f : info.flows()) {
      String n = normalizeFlowKeyword(f);
      if (n.contains("jiandao") || n.contains("jian_dao") || n.contains("剑道")) {
        return true;
      }
    }
    return false;
  }

  /** 检查玩家当前是否在使用“剑道”物品。 */
  public static boolean isUsingJiandao(Player player) {
    if (player == null || !player.isUsingItem()) {
      return false;
    }
    return isJiandaoItem(player.getUseItem());
  }
}
