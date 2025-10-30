package net.tigereye.chestcavity.registration;

import java.util.List;
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

  private static boolean initialised = false;

  private ActivationHookRegistry() {}

  /** 注册所有技能触发 Hook。 */
  public static void register() {
    if (initialised) {
      return;
    }
    initialised = true;

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
