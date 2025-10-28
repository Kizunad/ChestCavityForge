package net.tigereye.chestcavity.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian.WuxingGuiBianBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.WuxingHuaHenBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.WuxingHuaHenTuning;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/**
 * 组合杀招注册表
 * 管理需要多个器官组合才能释放的强力技能
 */
public final class ComboSkillRegistry {

  /**
   * 组合杀招条目
   * @param skillId 技能ID
   * @param displayName 显示名称（如"五行归变·逆转"）
   * @param iconLocation 图标ResourceLocation（完整路径，如 "guzhenren:textures/skill/wuxing_gui_bian.png"）
   *                     对应文件路径：assets/guzhenren/textures/skill/wuxing_gui_bian.png
   * @param requiredOrgans 必需器官列表（必须全部装备）
   * @param optionalOrgans 可选器官列表（装备任意一个即可触发，装备越多加成越强）
   * @param category 一级分类（如"变化道杀招"）
   * @param subcategory 二级分类（如"道痕转化"）
   * @param description 描述文本
   * @param tags 搜索标签
   * @param sourceHint 源码提示
   */
  public record ComboSkillEntry(
      ResourceLocation skillId,
      String displayName,
      ResourceLocation iconLocation,
      List<ResourceLocation> requiredOrgans,
      List<ResourceLocation> optionalOrgans,
      String category,
      String subcategory,
      String description,
      List<String> tags,
      String sourceHint) {
    public ComboSkillEntry {
      requiredOrgans = List.copyOf(requiredOrgans);
      optionalOrgans = List.copyOf(optionalOrgans);
      tags = List.copyOf(tags);
    }
  }

  /**
   * 器官装备检查结果
   * @param canActivate 是否可以激活（满足最低要求）
   * @param equippedRequired 已装备的必需器官数量
   * @param totalRequired 总必需器官数量
   * @param equippedOptional 已装备的可选器官数量
   * @param totalOptional 总可选器官数量
   * @param missingOrgans 缺少的器官列表
   */
  public record OrganCheckResult(
      boolean canActivate,
      int equippedRequired,
      int totalRequired,
      int equippedOptional,
      int totalOptional,
      List<ResourceLocation> missingOrgans) {
    public OrganCheckResult {
      missingOrgans = List.copyOf(missingOrgans);
    }
  }

  /**
   * 触发结果枚举
   */
  public enum TriggerResult {
    SUCCESS,           // 成功触发
    NOT_REGISTERED,    // 杀招未注册
    NO_CHEST_CAVITY,   // 玩家无胸腔
    MISSING_ORGAN,     // 缺少必需器官
    FAILED             // 触发失败（其他原因）
  }

  private static final Map<ResourceLocation, ComboSkillEntry> ENTRIES = new LinkedHashMap<>();
  private static boolean bootstrapped = false;

  private ComboSkillRegistry() {}

  public static void bootstrap() {
    if (bootstrapped) {
      return;
    }
    bootstrapped = true;

    // 五行归变·逆转（变化道杀招）
    register(
        "guzhenren:wuxing_gui_bian",
        "五行归变·逆转",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/wuxing_gui_bian.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "道痕转化",
        "将五行道痕逆转回变化道。支持暂时/永久模式，锚点越多税率越低（最多5个锚=免税），联动阴阳身享额外减税",
        tags("组合", "道痕", "五行", "逆转", "变化道"),
        "compat/guzhenren/item/combo/wuxing/gui_bian/WuxingGuiBianBehavior.java",
        () -> {
          Object unused = WuxingGuiBianBehavior.INSTANCE;
        });

    register(
        "guzhenren:wuxing_gui_bian_config",
        "五行归变·配置",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/wuxing_gui_bian.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "配置",
        "五行归变·逆转的配置入口，可切换暂时/永久模式并查看暂时模式冻结返还状态",
        tags("组合", "配置", "道痕"),
        "compat/guzhenren/item/combo/wuxing/gui_bian/WuxingGuiBianBehavior.java",
        () -> {
          Object unused = WuxingGuiBianBehavior.INSTANCE;
        });

    register(
        "guzhenren:wuxing_hua_hen",
        "五行化痕",
        WuxingHuaHenTuning.ICON,
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "道痕转化",
        "消耗变化道痕转化为五行道痕（金/木/水/炎/土），支持多种比例与固定量模式，阴阳模式享受税减",
        tags("组合", "道痕", "五行", "逆转", "变化道"),
        "compat/guzhenren/item/combo/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        });

    register(
        "guzhenren:wuxing_hua_hen_undo",
        "五行化痕·撤销",
        WuxingHuaHenTuning.ICON,
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "撤销",
        "10 分钟窗口内可撤销上次转化，返还 80% 已转化的变化道痕",
        tags("组合", "撤销", "道痕"),
        "compat/guzhenren/item/combo/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        });

    register(
        "guzhenren:wuxing_hua_hen_check",
        "五行化痕·查询",
        WuxingHuaHenTuning.ICON,
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "查询",
        "检查当前撤销窗口状态，显示可撤销的道痕转化记录与剩余时间",
        tags("组合", "查询", "道痕"),
        "compat/guzhenren/item/combo/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        });

    register(
        "guzhenren:wuxing_hua_hen_config",
        "五行化痕·配置",
        WuxingHuaHenTuning.ICON,
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu")),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu")),
        "变化道杀招",
        "配置",
        "五行化痕的配置入口，可点击切换目标元素及转化模式",
        tags("组合", "配置", "道痕"),
        "compat/guzhenren/item/combo/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        });

    ChestCavity.LOGGER.info("[ComboSkillRegistry] Registered {} combo skills", ENTRIES.size());
  }

  private static void register(
      String skillId,
      String displayName,
      ResourceLocation iconLocation,
      List<ResourceLocation> requiredOrgans,
      List<ResourceLocation> optionalOrgans,
      String category,
      String subcategory,
      String description,
      List<String> tags,
      String sourceHint) {
    register(
        skillId,
        displayName,
        iconLocation,
        requiredOrgans,
        optionalOrgans,
        category,
        subcategory,
        description,
        tags,
        sourceHint,
        null);
  }

  private static void register(
      String skillId,
      String displayName,
      ResourceLocation iconLocation,
      List<ResourceLocation> requiredOrgans,
      List<ResourceLocation> optionalOrgans,
      String category,
      String subcategory,
      String description,
      List<String> tags,
      String sourceHint,
      Runnable initializer) {
    ResourceLocation id = ResourceLocation.parse(skillId);
    if (initializer != null) {
      ActivationBootstrap.register(id, initializer);
    }
    ComboSkillEntry previous =
        ENTRIES.put(
            id,
            new ComboSkillEntry(
                id,
                displayName,
                iconLocation,
                requiredOrgans,
                optionalOrgans,
                category,
                subcategory,
                description,
                tags,
                sourceHint));
    if (previous != null) {
      ChestCavity.LOGGER.warn(
          "[ComboSkillRegistry] Duplicate registration for {}", id);
    }
  }

  private static List<String> tags(String... values) {
    List<String> list = new ArrayList<>(values.length);
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        list.add(value);
      }
    }
    return list;
  }

  public static Optional<ComboSkillEntry> get(ResourceLocation skillId) {
    bootstrap();
    return Optional.ofNullable(ENTRIES.get(skillId));
  }

  public static Collection<ComboSkillEntry> entries() {
    bootstrap();
    return Collections.unmodifiableCollection(ENTRIES.values());
  }

  public static boolean isSkillRegistered(ResourceLocation skillId) {
    bootstrap();
    return ENTRIES.containsKey(skillId);
  }

  /**
   * 检查玩家是否装备了激活该杀招所需的器官
   * @param player 玩家
   * @param entry 杀招条目
   * @return 检查结果
   */
  public static OrganCheckResult checkOrgans(Player player, ComboSkillEntry entry) {
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return new OrganCheckResult(
          false, 0, entry.requiredOrgans().size(), 0, entry.optionalOrgans().size(), List.of());
    }

    List<ResourceLocation> missingOrgans = new ArrayList<>();
    int equippedRequired = 0;
    int equippedOptional = 0;

    // 检查必需器官
    for (ResourceLocation organId : entry.requiredOrgans()) {
      if (hasOrgan(cc, organId)) {
        equippedRequired++;
      } else {
        missingOrgans.add(organId);
      }
    }

    // 检查可选器官（统计装备了多少个）
    for (ResourceLocation organId : entry.optionalOrgans()) {
      if (hasOrgan(cc, organId)) {
        equippedOptional++;
      }
    }

    // 必需器官全部装备 且 至少有一个可选器官
    boolean canActivate = (equippedRequired == entry.requiredOrgans().size()) && (equippedOptional > 0);

    return new OrganCheckResult(
        canActivate,
        equippedRequired,
        entry.requiredOrgans().size(),
        equippedOptional,
        entry.optionalOrgans().size(),
        missingOrgans);
  }

  private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || organId == null) {
      return false;
    }
    Item item = BuiltInRegistries.ITEM.getOptional(organId).orElse(null);
    if (item == null) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        return true;
      }
    }
    return false;
  }

  /**
   * 统计玩家装备了多少个指定列表中的器官
   * @param player 玩家
   * @param organIds 器官ID列表
   * @return 装备数量
   */
  public static int countEquippedOrgans(Player player, List<ResourceLocation> organIds) {
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return 0;
    }
    int count = 0;
    for (ResourceLocation organId : organIds) {
      if (hasOrgan(cc, organId)) {
        count++;
      }
    }
    return count;
  }

  /**
   * 触发组合杀招
   * @param player 服务端玩家
   * @param skillId 杀招ID
   * @return 触发结果
   */
  public static TriggerResult trigger(ServerPlayer player, ResourceLocation skillId) {
    bootstrap();
    ComboSkillEntry entry = ENTRIES.get(skillId);
    if (entry == null) {
      return TriggerResult.NOT_REGISTERED;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return TriggerResult.NO_CHEST_CAVITY;
    }

    // 检查器官装备条件
    OrganCheckResult organCheck = checkOrgans(player, entry);
    if (!organCheck.canActivate()) {
      return TriggerResult.MISSING_ORGAN;
    }

    // 通过 OrganActivationListeners 触发杀招
    boolean activated = OrganActivationListeners.activate(skillId, cc);
    return activated ? TriggerResult.SUCCESS : TriggerResult.FAILED;
  }
}
