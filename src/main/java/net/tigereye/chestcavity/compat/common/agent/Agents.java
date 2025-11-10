package net.tigereye.chestcavity.compat.common.agent;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.AbsorptionHelper;

/** Agent 工具库： - 构造 Agent 封装 - 抽象通用操作（属性、吸收、资源、流派标签收集） */
public final class Agents {

  private Agents() {}

  /** 从任何实体尝试获取 Agent（仅 LivingEntity 才可用）。 */
  public static Optional<Agent> from(Entity entity) {
    if (entity instanceof LivingEntity living) {
      return Optional.of(new Agent(living));
    }
    return Optional.empty();
  }

  /** 从 LivingEntity 构造 Agent。 */
  public static Agent of(LivingEntity living) {
    return new Agent(living);
  }

  /** 当前服务器 tick（仅服务端有效）。 */
  public static long serverTime(LivingEntity living) {
    return living.level() instanceof ServerLevel sl ? sl.getServer().getTickCount() : 0L;
  }

  /** 统一访问胸腔实例（玩家/NPC 一致）。 */
  public static ChestCavityInstance chestCavity(LivingEntity living) {
    return ChestCavityEntity.of(living).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
  }

  /** 打开 Guzhenren 资源句柄。 */
  public static Optional<ResourceHandle> openResource(LivingEntity living) {
    return GuzhenrenResourceBridge.open(living);
  }

  /** 安装/替换一次性 transient 属性修饰器。 */
  public static void applyTransientAttribute(
      LivingEntity entity,
      Holder<Attribute> attribute,
      ResourceLocation id,
      double amount,
      AttributeModifier.Operation op) {
    if (entity == null || attribute == null || id == null) return;
    AttributeInstance inst = entity.getAttribute(attribute);
    if (inst == null) return;
    AttributeModifier mod =
        new AttributeModifier(id, amount, op == null ? AttributeModifier.Operation.ADD_VALUE : op);
    AttributeOps.replaceTransient(inst, id, mod);
  }

  /** 移除指定 ID 的属性修饰器。 */
  public static void removeAttribute(
      LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
    if (entity == null || attribute == null || id == null) return;
    AttributeInstance inst = entity.getAttribute(attribute);
    if (inst != null) {
      AttributeOps.removeById(inst, id);
    }
  }

  /** 统一护盾应用：自动保证 MAX_ABSORPTION 容量，支持 onlyIncrease。 */
  public static float applyAbsorption(
      LivingEntity living, double amount, ResourceLocation modifierId, boolean onlyIncrease) {
    return AbsorptionHelper.applyAbsorption(living, amount, modifierId, onlyIncrease);
  }

  /** 收集一次胸腔库存中的“流派标签”信息（玩家/NPC 一致）。 */
  public static java.util.List<GuzhenrenFlowTooltipResolver.FlowInfo> collectInventoryFlows(
      LivingEntity living) {
    ChestCavityInstance cc = chestCavity(living);
    if (cc == null || cc.inventory == null) {
      return java.util.List.of();
    }
    java.util.ArrayList<GuzhenrenFlowTooltipResolver.FlowInfo> flows = new java.util.ArrayList<>();
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      GuzhenrenFlowTooltipResolver.FlowInfo info = GuzhenrenFlowTooltipResolver.inspect(stack);
      if (info.hasFlow()) flows.add(info);
    }
    return flows;
  }

  // 资源操作透传（便于统一调用习惯）
  public static java.util.OptionalDouble tryConsumeScaledZhenyuan(
      LivingEntity entity, double baseCost) {
    return ResourceOps.tryConsumeScaledZhenyuan(entity, baseCost);
  }

  public static java.util.OptionalDouble tryConsumeScaledJingli(
      LivingEntity entity, double baseCost) {
    return ResourceOps.tryConsumeScaledJingli(entity, baseCost);
  }

  public static boolean drainHealth(LivingEntity entity, float amount) {
    return ResourceOps.drainHealth(entity, amount);
  }
}
