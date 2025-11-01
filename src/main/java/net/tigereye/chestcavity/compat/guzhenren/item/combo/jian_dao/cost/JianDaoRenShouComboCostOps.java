package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.JianDaoComboRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoRenShouComboTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 葬生飞剑召令的资源/器官扣费封装。与正道版本相似，但独立使用 RenShou Tuning。
 */
public final class JianDaoRenShouComboCostOps {

  private JianDaoRenShouComboCostOps() {}

  /**
   * 执行实际扣费与器官移除（服务端）。需要在“存在至少一名村民”之后调用。
   * @return 成功与否
   */
  public static boolean tryPayAndConsume(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null || player.level().isClientSide()) return false;

    // 1) 器官存在性检查
    if (!hasAllRequiredOrgans(cc)) return false;

    // 2) 资源扣除（记录以便回滚）
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) return false;
    ResourceHandle handle = handleOpt.get();

    double spentZhenyuanMax = 0.0;
    boolean spentMaxJingli = false;
    boolean spentMaxHunpo = false;
    boolean spentDaoHen = false;
    float prevHealth = player.getHealth();
    boolean success = false;
    try {
      // 真元上限
      if (JianDaoRenShouComboTuning.COST_ZHENYUAN_BASE > 0.0) {
        double cap = handle.read(JianDaoRenShouComboTuning.KEY_MAX_ZHENYUAN).orElse(0.0);
        if (cap < JianDaoRenShouComboTuning.COST_ZHENYUAN_BASE) return false;
        OptionalDouble c = ResourceOps.tryAdjustDouble(
            handle,
            JianDaoRenShouComboTuning.KEY_MAX_ZHENYUAN,
            -JianDaoRenShouComboTuning.COST_ZHENYUAN_BASE,
            true,
            null);
        if (c.isEmpty()) return false;
        spentZhenyuanMax = JianDaoRenShouComboTuning.COST_ZHENYUAN_BASE;
      }

      // 精力上限
      if (JianDaoRenShouComboTuning.COST_JINGLI > 0.0) {
        double cap = handle.read(JianDaoRenShouComboTuning.KEY_MAX_JINGLI).orElse(0.0);
        if (cap < JianDaoRenShouComboTuning.COST_JINGLI) return false;
        OptionalDouble c = ResourceOps.tryAdjustDouble(
            handle,
            JianDaoRenShouComboTuning.KEY_MAX_JINGLI,
            -JianDaoRenShouComboTuning.COST_JINGLI,
            true,
            null);
        if (c.isEmpty()) return false;
        spentMaxJingli = true;
      }

      // 魂魄上限
      if (JianDaoRenShouComboTuning.COST_HUNPO > 0.0) {
        double cap = handle.read(JianDaoRenShouComboTuning.KEY_MAX_HUNPO).orElse(0.0);
        if (cap < JianDaoRenShouComboTuning.COST_HUNPO) return false;
        OptionalDouble c = ResourceOps.tryAdjustDouble(
            handle,
            JianDaoRenShouComboTuning.KEY_MAX_HUNPO,
            -JianDaoRenShouComboTuning.COST_HUNPO,
            true,
            null);
        if (c.isEmpty()) return false;
        spentMaxHunpo = true;
      }

      // 最大生命（通过属性修饰符）
      if (JianDaoRenShouComboTuning.COST_HEALTH > 0.0f) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return false;
        double currentMax = attr.getValue();
        if (currentMax <= JianDaoRenShouComboTuning.COST_HEALTH + 1.0f) return false;
        AttributeModifier mod = new AttributeModifier(
            JianDaoRenShouComboTuning.MAX_HEALTH_MODIFIER_ID,
            -JianDaoRenShouComboTuning.COST_HEALTH,
            AttributeModifier.Operation.ADD_VALUE);
        attr.removeModifier(JianDaoRenShouComboTuning.MAX_HEALTH_MODIFIER_ID);
        attr.addPermanentModifier(mod);
        double newMax = attr.getValue();
        if (player.getHealth() > newMax) player.setHealth((float) newMax);
      }

      // 道痕
      if (JianDaoRenShouComboTuning.COST_DAOHEN > 0.0) {
        if (!DaoHenResourceOps.consume(handle, JianDaoRenShouComboTuning.KEY_DAOHEN,
            JianDaoRenShouComboTuning.COST_DAOHEN)) return false;
        spentDaoHen = true;
      }

      // 3) 移除四件必需器官
      if (!removeRequiredOrgans(cc)) {
        rollback(handle, spentZhenyuanMax, spentMaxJingli, spentMaxHunpo, spentDaoHen);
        player.setHealth(prevHealth);
        return false;
      }

      success = true;
      return true;
    } finally {
      if (!success) {
        rollback(handle, spentZhenyuanMax, spentMaxJingli, spentMaxHunpo, spentDaoHen);
        player.setHealth(prevHealth);
      }
    }
  }

  private static boolean hasAllRequiredOrgans(ChestCavityInstance cc) {
    for (ResourceLocation id : requiredOrgans()) {
      if (!hasOrgan(cc, id)) return false;
    }
    return true;
  }

  private static boolean removeRequiredOrgans(ChestCavityInstance cc) {
    for (ResourceLocation id : requiredOrgans()) {
      if (!removeOne(cc, id)) return false;
    }
    return true;
  }

  private static List<ResourceLocation> requiredOrgans() {
    List<ResourceLocation> list = new ArrayList<>(4);
    list.add(JianDaoComboRegistry.REN_SHOU_ZANG_SHENG_GU);
    list.add(JianDaoComboRegistry.YU_JUN_GU);
    list.add(JianDaoComboRegistry.JIAN_HEN_GU);
    list.add(JianDaoComboRegistry.JIAN_JI_GU);
    return list;
  }

  private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) return false;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) return true;
    }
    return false;
  }

  private static boolean removeOne(ChestCavityInstance cc, ResourceLocation id) {
    Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    if (item == null) return false;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        stack.shrink(1);
        if (stack.isEmpty()) cc.inventory.setItem(i, ItemStack.EMPTY);
        return true;
      }
    }
    return false;
  }

  private static void rollback(
      ResourceHandle handle,
      double spentZhenyuanMax,
      boolean spentMaxJingli,
      boolean spentMaxHunpo,
      boolean spentDaoHen) {
    if (handle == null) return;
    if (spentZhenyuanMax > 0.0) {
      ResourceOps.tryAdjustDouble(handle, JianDaoRenShouComboTuning.KEY_MAX_ZHENYUAN, spentZhenyuanMax, true, null);
    }
    if (spentMaxJingli) {
      ResourceOps.tryAdjustDouble(handle, JianDaoRenShouComboTuning.KEY_MAX_JINGLI, JianDaoRenShouComboTuning.COST_JINGLI, true, null);
    }
    if (spentMaxHunpo) {
      ResourceOps.tryAdjustDouble(handle, JianDaoRenShouComboTuning.KEY_MAX_HUNPO, JianDaoRenShouComboTuning.COST_HUNPO, true, null);
    }
    if (spentDaoHen) {
      DaoHenResourceOps.add(handle, JianDaoRenShouComboTuning.KEY_DAOHEN, JianDaoRenShouComboTuning.COST_DAOHEN);
    }
  }
}

