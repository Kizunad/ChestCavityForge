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
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian.WuxingGuiBianBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.behavior.ShouPiFasciaLatchBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.behavior.ShouPiQianJiaCrashBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.behavior.ShouPiRollEvasionBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.behavior.ShouPiStoicReleaseBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.WuxingHuaHenBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.WuxingHuaHenTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.behavior.DualStrikeBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.behavior.RecallBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.behavior.TaiJiSwapBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.behavior.TransferBehavior;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry.CooldownHint;

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
      List<String> optionalFlows,
      String category,
      String subcategory,
      String description,
      List<String> tags,
      String sourceHint,
      CooldownHint cooldownHint) {
    public ComboSkillEntry {
      requiredOrgans = List.copyOf(requiredOrgans);
      optionalOrgans = List.copyOf(optionalOrgans);
      optionalFlows = List.copyOf(optionalFlows);
      tags = List.copyOf(tags);
      cooldownHint = cooldownHint != null ? cooldownHint : DEFAULT_COOLDOWN_HINT;
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
  private static final CooldownHint DEFAULT_COOLDOWN_HINT =
      CooldownHint.useOrgan("技能就绪", null);
  private static boolean bootstrapped = false;

  private ComboSkillRegistry() {}

  public static void bootstrap() {
    if (bootstrapped) {
      return;
    }
    bootstrapped = true;

    // 兽皮蛊组合杀招
    register(
        "guzhenren:shou_pi_roll_evasion",
        "皮走滚袭",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/shou_pi_roll.png"),
        List.of(ShouPiGuOrganBehavior.ORGAN_ID),
        List.of(
            ShouPiGuOrganBehavior.HUPI_GU_ID, ShouPiGuOrganBehavior.TIE_GU_GU_ID),
        "变化道杀招",
        "机动",
        "翻滚突进刷新厚皮窗口，并对最近敌人施加缓速与短时减伤窗",
        tags("变化道", "机动", "防御", "兽皮", "combo"),
        "compat/guzhenren/item/combo/bian_hua/shou_pi/roll/behavior/ShouPiRollEvasionBehavior.java",
        () -> {
          Object unused = ShouPiRollEvasionBehavior.INSTANCE;
        },
        null);

    register(
        "guzhenren:shou_pi_fascia_latch",
        "筋膜锁扣",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/shou_pi_fascia.png"),
        List.of(ShouPiGuOrganBehavior.ORGAN_ID),
        List.of(
            ShouPiGuOrganBehavior.HUPI_GU_ID, ShouPiGuOrganBehavior.TIE_GU_GU_ID),
        "变化道杀招",
        "防御",
        "引爆筋膜计数，获得额外减伤与护盾；铁骨强化冲击波，虎皮授予短暂坚韧",
        tags("变化道", "防御", "护盾", "兽皮", "combo"),
        "compat/guzhenren/item/combo/bian_hua/shou_pi/fascia_latch/behavior/ShouPiFasciaLatchBehavior.java",
        () -> {
          Object unused = ShouPiFasciaLatchBehavior.INSTANCE;
        },
        null);

    register(
        "guzhenren:shou_pi_stoic_release",
        "坚忍释放",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/shou_pi_stoic.png"),
        List.of(ShouPiGuOrganBehavior.ORGAN_ID),
        List.of(
            ShouPiGuOrganBehavior.HUPI_GU_ID, ShouPiGuOrganBehavior.TIE_GU_GU_ID),
        "变化道杀招",
        "防御",
        "释放坚忍层数，短窗强力减伤并获得护盾，额外强化下一次软反",
        tags("变化道", "防御", "护盾", "兽皮", "combo"),
        "compat/guzhenren/item/combo/bian_hua/shou_pi/stoic_release/behavior/ShouPiStoicReleaseBehavior.java",
        () -> {
          Object unused = ShouPiStoicReleaseBehavior.INSTANCE;
        },
        null);

    register(
        "guzhenren:shou_pi_qian_jia_crash",
        "嵌甲冲撞",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/shou_pi_crash.png"),
        List.of(ShouPiGuOrganBehavior.ORGAN_ID),
        List.of(
            ShouPiGuOrganBehavior.HUPI_GU_ID, ShouPiGuOrganBehavior.TIE_GU_GU_ID),
        "变化道杀招",
        "爆发",
        "突进并引爆软反池造成范围真实伤害，随后获得极短免疫窗",
        tags("变化道", "输出", "防御", "兽皮", "combo"),
        "compat/guzhenren/item/combo/bian_hua/shou_pi/qian_jia_crash/behavior/ShouPiQianJiaCrashBehavior.java",
        () -> {
          Object unused = ShouPiQianJiaCrashBehavior.INSTANCE;
        },
        null);

    // 水/奴联动：鱼群（组合版）
    register(
        "guzhenren:yu_qun_combo",
        "鱼群·组合",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/yu_qun_zu_he.png"),
        List.of(ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu")),
        List.of(),
        List.of("水道", "奴道"),
        "变化道杀招",
        "水/奴联动",
        "水灵齐射的组合形态：随水/奴器官数量增强射程、宽度与控制",
        tags("组合", "水道", "奴道", "控制"),
        "compat/guzhenren/item/combo/bian_hua/yu_qun/behavior/YuQunComboBehavior.java",
        () -> {
          try {
            Class.forName(
                "net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.behavior.YuQunComboBehavior");
          } catch (Throwable ignored) {}
        },
        null);

    // 水/奴联动：饵祭召鲨（组合版）
    register(
        "guzhenren:yu_shi_summon_combo",
        "饵祭召鲨·组合",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/er_ji_zhao_sha.png"),
        List.of(ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu")),
        List.of(),
        List.of("水道", "奴道"),
        "变化道杀招",
        "水/奴联动",
        "召唤协战鲨鱼的组合形态：水道增强续航，奴道提升服从与编队管理",
        tags("组合", "水道", "奴道", "召唤"),
        "compat/guzhenren/item/combo/bian_hua/yu_shi/behavior/YuShiSummonComboBehavior.java",
        () -> {
          try {
            Class.forName(
                "net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.behavior.YuShiSummonComboBehavior");
          } catch (Throwable ignored) {}
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/gui_bian/WuxingGuiBianBehavior.java",
        () -> {
          Object unused = WuxingGuiBianBehavior.INSTANCE;
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/gui_bian/WuxingGuiBianBehavior.java",
        () -> {
          Object unused = WuxingGuiBianBehavior.INSTANCE;
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        },
        null);

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
        "compat/guzhenren/item/combo/bian_hua/wuxing/hua_hen/WuxingHuaHenBehavior.java",
        () -> {
          Object unused = WuxingHuaHenBehavior.INSTANCE;
        },
        null);

    // 阴阳转身蛊组合杀招
    register(
        "guzhenren:yin_yang_tai_ji_swap",
        "太极错位",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/ying_yang_zhuan_sheng_tai_ji_swap.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu")),
        List.of(),
        "变化道杀招",
        "位移",
        "与另一形态上次所在的位置瞬移互换，并获得短暂无敌。是集机动与保命于一体的核心位移技能。",
        tags("变化道", "位移", "保命", "阴阳"),
        "compat/guzhenren/item/combo/bian_hua/yin_yang/tai_ji_swap/behavior/TaiJiSwapBehavior.java",
        () -> {
          TaiJiSwapBehavior.initialize();
        },
        null);

    register(
        "guzhenren:yin_yang_dual_strike",
        "两界同击",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/ying_yang_zhuan_sheng_dual_strike.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu")),
        List.of(),
        "变化道杀招",
        "爆发",
        "短时间内用阴阳双身连续攻击同一目标，引爆毁灭性的投影伤害。",
        tags("变化道", "爆发", "输出", "阴阳"),
        "compat/guzhenren/item/combo/bian_hua/yin_yang/dual_strike/behavior/DualStrikeBehavior.java",
        () -> {
          DualStrikeBehavior.initialize();
        },
        null);

    register(
        "guzhenren:yin_yang_transfer",
        "阴阳互渡",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/ying_yang_zhuan_sheng_transfer.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu")),
        List.of(),
        "变化道杀招",
        "资源管理",
        "在双形态的资源池之间转移30%的资源，实现灵活的战术续航。",
        tags("变化道", "资源管理", "续航", "阴阳"),
        "compat/guzhenren/item/combo/bian_hua/yin_yang/transfer/behavior/TransferBehavior.java",
        () -> {
          TransferBehavior.initialize();
        },
        null);

    register(
        "guzhenren:yin_yang_recall",
        "归位",
        ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/ying_yang_zhuan_sheng_recall.png"),
        List.of(
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu"),
            ResourceLocation.fromNamespaceAndPath("guzhenren", "hongbiangu")),
        List.of(),
        "变化道杀招",
        "保命",
        "瞬移至另一形态的锚点，并清除仇恨，是绝境逢生的关键。",
        tags("变化道", "位移", "保命", "阴阳", "传送"),
        "compat/guzhenren/item/combo/bian_hua/yin_yang/recall/behavior/RecallBehavior.java",
        () -> {
          RecallBehavior.initialize();
        },
        null);

    ChestCavity.LOGGER.info("[ComboSkillRegistry] Registered {} combo skills", ENTRIES.size());
  }

  private static void register(
      String skillId,
      String displayName,
      ResourceLocation iconLocation,
      List<ResourceLocation> requiredOrgans,
      List<ResourceLocation> optionalOrgans,
      List<String> optionalFlows,
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
        optionalFlows,
        category,
        subcategory,
        description,
        tags,
        sourceHint,
        null,
        null);
  }

  // 兼容旧签名（无 optionalFlows）：默认 optionalFlows 为空
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
        List.of(),
        category,
        subcategory,
        description,
        tags,
        sourceHint,
        null,
        null);
  }

  private static void register(
      String skillId,
      String displayName,
      ResourceLocation iconLocation,
      List<ResourceLocation> requiredOrgans,
      List<ResourceLocation> optionalOrgans,
      List<String> optionalFlows,
      String category,
      String subcategory,
      String description,
      List<String> tags,
      String sourceHint,
      Runnable initializer,
      CooldownHint cooldownHint) {
    ResourceLocation id = ResourceLocation.parse(skillId);
    if (initializer != null) {
      ActivationBootstrap.register(id, initializer);
    }
    CooldownHint resolvedHint = cooldownHint != null ? cooldownHint : DEFAULT_COOLDOWN_HINT;
    ComboSkillEntry previous =
        ENTRIES.put(
            id,
            new ComboSkillEntry(
                id,
                displayName,
                iconLocation,
                requiredOrgans,
                optionalOrgans,
                optionalFlows,
                category,
                subcategory,
                description,
                tags,
                sourceHint,
                resolvedHint));
    if (previous != null) {
      ChestCavity.LOGGER.warn(
          "[ComboSkillRegistry] Duplicate registration for {}", id);
    }
  }

  // 兼容旧签名（无 optionalFlows）：默认 optionalFlows 为空
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
      Runnable initializer,
      CooldownHint cooldownHint) {
    register(
        skillId,
        displayName,
        iconLocation,
        requiredOrgans,
        optionalOrgans,
        List.of(),
        category,
        subcategory,
        description,
        tags,
        sourceHint,
        initializer,
        cooldownHint);
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

    // 检查可选流派（统计背包/胸腔中满足流派的器官数量）
    if (!entry.optionalFlows().isEmpty()) {
      equippedOptional += countFlowMatches(cc, player.level(), entry.optionalFlows());
    }

    // 必需器官全部装备；若配置有可选协同，则至少需要装备一个
    int totalOptional = entry.optionalOrgans().size() + entry.optionalFlows().size();
    boolean hasOptionalRequirement = totalOptional > 0;
    boolean canActivate =
        equippedRequired == entry.requiredOrgans().size()
            && (!hasOptionalRequirement || equippedOptional > 0);

    return new OrganCheckResult(
        canActivate,
        equippedRequired,
        entry.requiredOrgans().size(),
        equippedOptional,
        totalOptional,
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

  /** 统计“可选协同”总量：可选器官件数 + 可选流派匹配件数。 */
  public static int countOptionalSynergy(Player player, ComboSkillEntry entry) {
    int count = countEquippedOrgans(player, entry.optionalOrgans());
    if (!entry.optionalFlows().isEmpty()) {
      ChestCavityInstance cc =
          ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
      if (cc != null) {
        count += countFlowMatches(cc, player.level(), entry.optionalFlows());
      }
    }
    return count;
  }

  private static int countFlowMatches(
      ChestCavityInstance cc, net.minecraft.world.level.Level level, List<String> flows) {
    if (cc == null || cc.inventory == null || level == null || flows == null || flows.isEmpty()) {
      return 0;
    }
    int matched = 0;
    var context = net.minecraft.world.item.Item.TooltipContext.of(level);
    var flag = net.minecraft.world.item.TooltipFlag.NORMAL;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      var stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      var info = GuzhenrenFlowTooltipResolver.inspect(stack, context, flag, null);
      if (!info.hasFlow()) continue;
      for (String f : info.flows()) {
        if (f == null) continue;
        for (String key : flows) {
          if (key == null) continue;
          // 宽松匹配：同 Resolver.hasFlow 逻辑
          String fk = f.toLowerCase(java.util.Locale.ROOT);
          String kk = key.toLowerCase(java.util.Locale.ROOT);
          if (fk.contains(kk) || fk.replace("道", "").contains(kk.replace("道", ""))) {
            matched++;
            break;
          }
        }
        if (matched > 0 && matched == i + 1) {
          // noop; simple continue
        }
      }
    }
    return matched;
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

  public static void scheduleReadyToast(
      ServerPlayer player, ResourceLocation skillId, long readyAtTick, long nowTick) {
    if (player == null) {
      return;
    }
    bootstrap();
    ComboSkillEntry entry = ENTRIES.get(skillId);
    if (entry == null) {
      return;
    }
    CooldownHint hint = entry.cooldownHint();
    ResourceLocation iconId =
        hint.iconOverride() != null ? hint.iconOverride() : resolveDefaultIcon(entry);
    ItemStack iconStack = ItemStack.EMPTY;
    if (iconId != null) {
      Item item = BuiltInRegistries.ITEM.getOptional(iconId).orElse(null);
      if (item != null) {
        iconStack = new ItemStack(item);
      }
    }
    String title = hint.title();
    if (title == null || title.isBlank()) {
      title = "技能就绪";
    }
    String subtitle = hint.subtitle();
    if ((subtitle == null || subtitle.isBlank()) && !iconStack.isEmpty()) {
      subtitle = iconStack.getHoverName().getString();
    }
    CountdownOps.scheduleToastAt(
        player.serverLevel(), player, readyAtTick, nowTick, iconStack, title, subtitle);
  }

  private static ResourceLocation resolveDefaultIcon(ComboSkillEntry entry) {
    if (entry == null) {
      return null;
    }
    if (!entry.requiredOrgans().isEmpty()) {
      return entry.requiredOrgans().get(0);
    }
    if (!entry.optionalOrgans().isEmpty()) {
      return entry.optionalOrgans().get(0);
    }
    return null;
  }
}
