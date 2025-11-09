package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.JianDaoComboRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.calculator.JianDaoComboCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.fx.JianDaoComboFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.messages.JianDaoComboMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;

/**
 * 剑道组合杀招（第一个）行为入口（占位）。
 *
 * <p>仅完成注册、依赖检查与冷却 Toast 安排；具体战斗逻辑后续补充。
 */
public final class JianDaoFirstComboBehavior {

  private static final ResourceLocation SKILL_ID = JianDaoComboRegistry.SKILL_ID;
  private static final String COOLDOWN_KEY = "JianDaoFirstComboReadyAt";

  private JianDaoFirstComboBehavior() {}

  /** 注册激活处理器。 */
  public static void initialize() {
    OrganActivationListeners.register(
        SKILL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    // 选择一个承载器官（按优先级）用于承载冷却与状态
    Optional<ItemStack> host = findHostOrgan(cc);
    if (host.isEmpty()) {
      // 按理在 ComboSkillRegistry.checkOrgans 已保障齐备；此处为兜底
      return;
    }

    ItemStack organ = host.get();
    OrganState state = OrganState.of(organ, "guzhenren:jian_dao_combo");
    MultiCooldown cooldown =
        MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.level().getGameTime();
    if (!cooldown.entry(COOLDOWN_KEY).isReady(now)) {
      return;
    }

    // 先扣费与移除四件必需器官（失败静默返回）
    if (!net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.cost.JianDaoComboCostOps
        .tryPayAndConsume(player, cc)) {
      return;
    }

    // 识别主手物品（无则视为空栈，继承为0）并生成“正道”飞剑（使用 Combo 自有系数）
    ItemStack source = player.getMainHandItem();

    // 构建系数配置（数值留在 Combo Tuning）
    var cfg = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.util
        .ItemAffinityUtil.Config();
    cfg.attackDamageCoef = JianDaoComboTuning.AFFINITY_ATTACK_DAMAGE_COEF;
    cfg.attackSpeedAbsCoef = JianDaoComboTuning.AFFINITY_ATTACK_SPEED_ABS_COEF;
    cfg.sharpnessDmgPerLvl = JianDaoComboTuning.AFFINITY_SHARPNESS_DMG_PER_LVL;
    cfg.sharpnessVelPerLvl = JianDaoComboTuning.AFFINITY_SHARPNESS_VEL_PER_LVL;
    cfg.unbreakingLossMultPerLvl = JianDaoComboTuning.AFFINITY_UNBREAKING_LOSS_MULT_PER_LVL;
    cfg.sweepingBase = JianDaoComboTuning.AFFINITY_SWEEPING_BASE;
    cfg.sweepingPerLvl = JianDaoComboTuning.AFFINITY_SWEEPING_PER_LVL;
    cfg.efficiencyBlockEffPerLvl = JianDaoComboTuning.AFFINITY_EFFICIENCY_BLOCK_EFF_PER_LVL;
    cfg.miningSpeedToBlockEffCoef = JianDaoComboTuning.AFFINITY_MINING_SPEED_TO_BLOCK_EFF;
    cfg.maxDamageToMaxDurabilityCoef = JianDaoComboTuning.AFFINITY_MAX_DAMAGE_TO_MAX_DURABILITY;
    cfg.armorToMaxDurabilityCoef = JianDaoComboTuning.AFFINITY_ARMOR_TO_MAX_DURABILITY;
    cfg.armorDuraLossMultPerPoint = JianDaoComboTuning.AFFINITY_ARMOR_DURA_LOSS_MULT_PER_POINT;

    var result =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.util
            .ItemAffinityUtil.evaluate(
                (net.minecraft.server.level.ServerLevel) player.level(), source, cfg);

    // Combo飞剑独立配置最大耐久度（完全覆盖默认值）
    result.modifiers.maxDurabilityOverride = JianDaoComboTuning.COMBO_SWORD_MAX_DURABILITY;

    net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity sword =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword
            .FlyingSwordSpawner.spawnFromOwnerWithModifiersAndSpec(
                (net.minecraft.server.level.ServerLevel) player.level(),
                player,
                source,
                net.tigereye.chestcavity.compat.guzhenren.flyingsword
                    .FlyingSwordType.ZHENG_DAO,
                result.modifiers,
                result.initSpec);

    if (sword == null) {
      // 生成失败则不进入窗口/冷却
      return;
    }

    // 生成成功：若有源物品则消耗1个
    if (!source.isEmpty()) {
      source.shrink(1);
    }

    // 开启窗口与提示（实际计算在 Calculator；效果在 Fx；消息在 Messages）
    JianDaoComboCalculator.State window = JianDaoComboCalculator.startWindow(now);
    JianDaoComboMessages.sendAction(player, JianDaoComboMessages.WINDOW_OPEN);
    JianDaoComboFx.playActivate(player);

    // 设置冷却并安排就绪提示
    long ready = now + JianDaoComboTuning.BASE_COOLDOWN_TICKS;
    cooldown.entry(COOLDOWN_KEY).setReadyAt(ready);
    ComboSkillRegistry.scheduleReadyToast(player, SKILL_ID, ready, now);
    // window 目前仅局部变量占位，后续扩展可写入 OrganState 存档
  }

  private static Optional<ItemStack> findHostOrgan(ChestCavityInstance cc) {
    List<ResourceLocation> priority =
        List.of(
            JianDaoComboRegistry.JIAN_QI_GU,
            JianDaoComboRegistry.ZHI_LU_GU,
            JianDaoComboRegistry.YU_JUN_GU,
            JianDaoComboRegistry.YI_ZHUAN_REN_DAO_XI_WANG_GU);
    for (ResourceLocation id : priority) {
      Item candidate = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(null);
      if (candidate == null) continue;
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack stack = cc.inventory.getItem(i);
        if (!stack.isEmpty() && stack.getItem() == candidate) {
          return Optional.of(stack);
        }
      }
    }
    return Optional.empty();
  }
}
