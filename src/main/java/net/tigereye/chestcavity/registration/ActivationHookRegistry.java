package net.tigereye.chestcavity.registration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.SkillActivationHooks;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.skill.effects.builtin.CompositeEffect;
import net.tigereye.chestcavity.skill.effects.builtin.ResourceFieldSnapshotEffect;

/**
 * 统一注册所有 SkillActivationHooks 扩展，避免在具体工具类内散落静态初始化逻辑。
 */
public final class ActivationHookRegistry {

  private static final Set<String> ENABLED_FAMILIES = new HashSet<>();
  private static boolean initialised = false;

  private ActivationHookRegistry() {}

  /**
   * Registers a family of passive abilities.
   *
   * @param id The ID of the family.
   */
  public static void registerFamily(String id) {
    ENABLED_FAMILIES.add(id);
  }

  /**
   * Checks if a family of passive abilities is enabled.
   *
   * @param id The ID of the family.
   * @return True if the family is enabled, false otherwise.
   */
  public static boolean isFamilyEnabled(String id) {
    return ENABLED_FAMILIES.contains(id);
  }

  /**
   * Gets the set of all enabled families.
   *
   * @return An unmodifiable set of enabled family IDs.
   */
  public static Set<String> getEnabledFamilies() {
    return Collections.unmodifiableSet(ENABLED_FAMILIES);
  }

  /** 注册所有技能触发 Hook。 */
  public static void register() {
    if (initialised) {
      return;
    }
    initialised = true;

    // Register default families
    registerFamily("liupai_shuidao");
    registerFamily("liupai_bianhuadao");
    registerFamily("daohen_shuidao");
    registerFamily("daohen_bianhuadao");
    registerFamily("daohen_yandao");
    registerFamily("liupai_bingxuedao");

    // 技能效果: 冰雪道技能集需要快照道痕与流派经验
    SkillEffectBus.register(
        "^guzhenren:(bing_ji_gu_.*|shuang_xi_gu_.*)$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect("bing_xue:", List.of("liupai_bingxuedao")),
            new net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ComputedBingXueDaohenEffect()
        ));

    // 技能效果: 饵祭召鲨需要先快照关键 Guzhenren 资源字段
    SkillEffectBus.register(
        "^guzhenren:yu_shi_summon_combo$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect(
                "yu_shi:",
                List.of(
                    "liupai_shuidao",
                    "liupai_bianhuadao",
                    "daohen_shuidao",
                    "daohen_bianhuadao",
                    "daohen_yandao"))));

    // 技能效果: 鱼群·组合 也需要同样的快照（用于参数与冷却按流派经验/道痕调整）
    SkillEffectBus.register(
        "^guzhenren:yu_qun_combo$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect(
                "yu_qun:",
                List.of(
                    "liupai_shuidao",
                    "liupai_bianhuadao",
                    "daohen_shuidao",
                    "daohen_bianhuadao",
                    "daohen_yandao"))));

    // 技能效果: 阴阳鸟技能集也需要同样的快照（用于参数与冷却按流派经验/道痕调整）
    SkillEffectBus.register(
        "^guzhenren:yin_yang_.*$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect(
                "yin_yang:",
                List.of("liupai_bianhuadao", "daohen_bianhuadao"))));

    // 技能效果: 兽皮技能集也需要同样的快照（用于参数与冷却按流派经验/道痕调整）
    SkillEffectBus.register(
        "^guzhenren:shou_pi_.*$",
        CompositeEffect.of(
            new ResourceFieldSnapshotEffect(
                "shou_pi:",
                List.of("liupai_bianhuadao", "daohen_bianhuadao"))));

    // 技能效果总线：前置/后置分发（默认无注册效果，零行为变更）
    SkillActivationHooks.registerActivePreHandler(
        "^guzhenren:.*$",
        (ServerPlayer player,
            ResourceLocation skillId,
            ChestCavityInstance cc,
            ActiveSkillRegistry.ActiveSkillEntry entry) -> {
          SkillEffectBus.pre(player, skillId, cc, entry);
          return SkillActivationHooks.ActivePreHookDecision.continueChain();
        });
    SkillActivationHooks.registerActivePostHandler(
        "^guzhenren:.*$",
        (player, skillId, cc, entry, result) ->
            SkillEffectBus.post(player, skillId, cc, entry, result));

    SkillActivationHooks.registerComboPreHandler(
        "^guzhenren:.*$",
        (ServerPlayer player,
            ResourceLocation skillId,
            ChestCavityInstance cc,
            net.tigereye.chestcavity.skill.ComboSkillRegistry.ComboSkillEntry entry) -> {
          // Combo 与 Active 共享总线
          SkillEffectBus.pre(player, skillId, cc, null);
          return SkillActivationHooks.ComboPreHookDecision.continueChain();
        });
    SkillActivationHooks.registerComboPostHandler(
        "^guzhenren:.*$",
        (player, skillId, cc, entry, result) ->
            SkillEffectBus.post(player, skillId, cc, null, mapComboResult(result)));

    SkillActivationHooks.registerActivePostHandler(
        "^guzhenren:.*$", GuzhenrenFlowActivationHooks::handleSkillPostActivation);
  }

  private static ActiveSkillRegistry.TriggerResult mapComboResult(
      net.tigereye.chestcavity.skill.ComboSkillRegistry.TriggerResult r) {
    return switch (r) {
      case SUCCESS -> ActiveSkillRegistry.TriggerResult.SUCCESS;
      case NOT_REGISTERED -> ActiveSkillRegistry.TriggerResult.NOT_REGISTERED;
      case NO_CHEST_CAVITY -> ActiveSkillRegistry.TriggerResult.NO_CHEST_CAVITY;
      case MISSING_ORGAN -> ActiveSkillRegistry.TriggerResult.MISSING_ORGAN;
      case FAILED -> ActiveSkillRegistry.TriggerResult.ABILITY_NOT_REGISTERED;
      case BLOCKED_BY_HANDLER -> ActiveSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER;
    };
  }
}
