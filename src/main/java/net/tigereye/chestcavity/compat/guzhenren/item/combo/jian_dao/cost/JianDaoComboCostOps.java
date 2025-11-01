package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.cost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.JianDaoComboRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 剑道 Combo 的资源与器官消耗封装。
 *
 * <p>职责：
 * - 检查并移除必需器官（四件）
 * - 使用 ResourceOps 扣除真元/精力/魂魄/生命与剑道道痕（失败则回滚）
 * - 成功后授予 5 分钟 虚弱 III
 *
 * <p>注意：单元测试仅覆盖纯函数（器官缺失判定与预检逻辑），避免依赖 MC 运行时。
 */
public final class JianDaoComboCostOps {

  private JianDaoComboCostOps() {}

  /** 纯函数：从给定已装备集合计算缺失的必需器官。 */
  public static List<ResourceLocation> missingRequiredOrgansFromIds(
      Set<ResourceLocation> equipped) {
    Set<ResourceLocation> have = equipped == null ? Set.of() : new HashSet<>(equipped);
    List<ResourceLocation> missing = new ArrayList<>(4);
    for (ResourceLocation req : requiredOrgans()) {
      if (!have.contains(req)) {
        missing.add(req);
      }
    }
    return missing;
  }

  /** 纯函数：基于“快照”预检资源是否足够（不执行扣除）。 */
  public static boolean precheckResources(
      double zhenyuanBaseBudget,
      double jingli,
      double hunpo,
      double health,
      double jiandaoDaoHen) {
    return (zhenyuanBaseBudget >= JianDaoComboTuning.COST_ZHENYUAN_BASE)
        && (jingli >= JianDaoComboTuning.COST_JINGLI)
        && (hunpo >= JianDaoComboTuning.COST_HUNPO)
        && (health > JianDaoComboTuning.COST_HEALTH + 1.0D) // 预留 1 HP 安全值
        && (jiandaoDaoHen >= JianDaoComboTuning.COST_JIANDAO_DAOHEN);
  }

  /**
   * 执行实际扣费与器官移除（服务端）。
   *
   * <p>流程：
   * 1) 检查必需器官存在 → 2) 资源扣除（可回滚）→ 3) 逐一移除器官（失败则回滚资源）→ 4) 授予虚弱 III
   *
   * @return 成功与否
   */
  public static boolean tryPayAndConsume(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null || player.level().isClientSide()) {
      return false;
    }

    // 1) 器官存在性检查
    List<ResourceLocation> missing = missingRequiredOrgans(cc);
    if (!missing.isEmpty()) {
      return false;
    }

    // 2) 资源扣除（记录以便回滚）
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    ResourceHandle handle = handleOpt.get();
    double spentZhenyuanMax = 0.0;
    boolean spentMaxJingli = false;
    boolean spentMaxHunpo = false;
    boolean spentDaoHen = false;
    float prevHealth = player.getHealth();
    boolean success = false;
    try {
      // 真元上限：扣最大可用上限（严格全额，不足则失败）
      if (JianDaoComboTuning.COST_ZHENYUAN_BASE > 0.0) {
        double cap = handle.read(JianDaoComboTuning.KEY_MAX_ZHENYUAN).orElse(0.0);
        if (cap < JianDaoComboTuning.COST_ZHENYUAN_BASE) {
          return false;
        }
        OptionalDouble c =
            ResourceOps.tryAdjustDouble(
                handle, JianDaoComboTuning.KEY_MAX_ZHENYUAN, -JianDaoComboTuning.COST_ZHENYUAN_BASE,
                true, null);
        if (c.isEmpty()) {
          return false;
        }
        spentZhenyuanMax = JianDaoComboTuning.COST_ZHENYUAN_BASE;
      }

      // 精力上限
      if (JianDaoComboTuning.COST_JINGLI > 0.0) {
        double cap = handle.read(JianDaoComboTuning.KEY_MAX_JINGLI).orElse(0.0);
        if (cap < JianDaoComboTuning.COST_JINGLI) {
          return false;
        }
        OptionalDouble c =
            ResourceOps.tryAdjustDouble(
                handle,
                JianDaoComboTuning.KEY_MAX_JINGLI,
                -JianDaoComboTuning.COST_JINGLI,
                true,
                null);
        if (c.isEmpty()) {
          return false;
        }
        spentMaxJingli = true;
      }

      // 魂魄上限
      if (JianDaoComboTuning.COST_HUNPO > 0.0) {
        double cap = handle.read(JianDaoComboTuning.KEY_MAX_HUNPO).orElse(0.0);
        if (cap < JianDaoComboTuning.COST_HUNPO) {
          return false;
        }
        OptionalDouble c =
            ResourceOps.tryAdjustDouble(
                handle,
                JianDaoComboTuning.KEY_MAX_HUNPO,
                -JianDaoComboTuning.COST_HUNPO,
                true,
                null);
        if (c.isEmpty()) {
          return false;
        }
        spentMaxHunpo = true;
      }

      // 生命上限（通过永久属性修饰符）
      if (JianDaoComboTuning.COST_HEALTH > 0.0f) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) {
          return false;
        }
        double currentMax = attr.getValue();
        if (currentMax <= JianDaoComboTuning.COST_HEALTH + 1.0f) {
          return false;
        }
        AttributeModifier mod =
            new AttributeModifier(
                JianDaoComboTuning.MAX_HEALTH_MODIFIER_ID,
                -JianDaoComboTuning.COST_HEALTH,
                AttributeModifier.Operation.ADD_VALUE);
        // 先移除同名修饰符，确保叠加语义为替换
        attr.removeModifier(JianDaoComboTuning.MAX_HEALTH_MODIFIER_ID);
        attr.addPermanentModifier(mod);
        // 若当前生命超过新上限，进行钳制
        double newMax = attr.getValue();
        if (player.getHealth() > newMax) {
          player.setHealth((float) newMax);
        }
      }

      // 剑道道痕
      if (JianDaoComboTuning.COST_JIANDAO_DAOHEN > 0.0) {
        if (!DaoHenResourceOps.consume(handle, JianDaoComboTuning.KEY_DAOHEN_JIANDAO,
            JianDaoComboTuning.COST_JIANDAO_DAOHEN)) {
          return false;
        }
        spentDaoHen = true;
      }

      // 3) 移除四件器官（逐一）
      if (!removeRequiredOrgans(cc)) {
        // 回滚资源
        rollback(handle, spentZhenyuanMax, spentMaxJingli, spentMaxHunpo, spentDaoHen);
        player.setHealth(prevHealth);
        return false;
      }

      // 4) 授予虚弱 III
      player.addEffect(
          new MobEffectInstance(
              MobEffects.WEAKNESS,
              (int) JianDaoComboTuning.EFFECT_WEAKNESS_TICKS,
              JianDaoComboTuning.EFFECT_WEAKNESS_AMPLIFIER,
              false,
              true));

      success = true;
      return true;
    } finally {
      if (!success) {
        rollback(handle, spentZhenyuanMax, spentMaxJingli, spentMaxHunpo, spentDaoHen);
        player.setHealth(prevHealth);
      }
    }
  }

  private static void rollback(
      ResourceHandle handle,
      double spentZhenyuanMax,
      boolean spentMaxJingli,
      boolean spentMaxHunpo,
      boolean spentDaoHen) {
    if (handle == null) return;
    if (spentZhenyuanMax > 0.0) {
      ResourceOps.tryAdjustDouble(
          handle,
          JianDaoComboTuning.KEY_MAX_ZHENYUAN,
          spentZhenyuanMax,
          true,
          null);
    }
    if (spentMaxJingli) {
      ResourceOps.tryAdjustDouble(
          handle, JianDaoComboTuning.KEY_MAX_JINGLI, JianDaoComboTuning.COST_JINGLI, true, null);
    }
    if (spentMaxHunpo) {
      ResourceOps.tryAdjustDouble(
          handle, JianDaoComboTuning.KEY_MAX_HUNPO, JianDaoComboTuning.COST_HUNPO, true, null);
    }
    if (spentDaoHen) {
      DaoHenResourceOps.add(
          handle, JianDaoComboTuning.KEY_DAOHEN_JIANDAO, JianDaoComboTuning.COST_JIANDAO_DAOHEN);
    }
  }

  private static List<ResourceLocation> missingRequiredOrgans(ChestCavityInstance cc) {
    List<ResourceLocation> missing = new ArrayList<>(4);
    for (ResourceLocation req : requiredOrgans()) {
      if (!hasOrgan(cc, req)) {
        missing.add(req);
      }
    }
    return missing;
  }

  private static boolean removeRequiredOrgans(ChestCavityInstance cc) {
    // 逐个移除，确保每个器官都移除 1 个
    for (ResourceLocation id : requiredOrgans()) {
      if (!removeOne(cc, id)) {
        return false;
      }
    }
    return true;
  }

  private static boolean removeOne(ChestCavityInstance cc, ResourceLocation id) {
    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) return false;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        stack.shrink(1);
        if (stack.isEmpty()) {
          cc.inventory.setItem(i, ItemStack.EMPTY);
        }
        return true;
      }
    }
    return false;
  }

  private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) return false;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        return true;
      }
    }
    return false;
  }

  private static List<ResourceLocation> requiredOrgans() {
    return List.of(
        JianDaoComboRegistry.ZHI_LU_GU,
        JianDaoComboRegistry.YU_JUN_GU,
        JianDaoComboRegistry.YI_ZHUAN_REN_DAO_XI_WANG_GU,
        JianDaoComboRegistry.JIAN_QI_GU);
  }
}
