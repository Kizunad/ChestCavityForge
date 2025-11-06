package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.api.ShockfieldMath;
import net.tigereye.chestcavity.compat.guzhenren.shockfield.runtime.ShockfieldManager;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 四转·剑荡蛊（Shockfield）器官行为实现。
 *
 * <p>功能：
 *
 * <ul>
 *   <li>OnHit 触发：玩家/飞剑命中时创建波源
 *   <li>维持态消耗：按秒扣费（念头/精力），资源不足即时熄灭
 *   <li>波传播与干涉：圆形波前外扩、振幅指数衰减、周期渐慢
 *   <li>命中与伤害结算：友伤排除、同帧自波排除、目标-波命中节流
 * </ul>
 */
public final class JianDangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganOnHitListener, OrganSlowTickListener {

  public static final JianDangGuOrganBehavior INSTANCE = new JianDangGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiandanggu");
  private static final String STATE_ROOT = "JianDangGu";

  private JianDangGuOrganBehavior() {}

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (target == null || !target.isAlive() || target == attacker) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    // 资源门槛：4转4阶段（Burst门槛）
    try {
      var handleOpt = net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(player);
      if (handleOpt.isEmpty()) {
        return damage;
      }
      var h = handleOpt.get();
      int zhuanshu = (int) Math.floor(h.read("zhuanshu").orElse(0.0));
      int jieduan = (int) Math.floor(h.read("jieduan").orElse(0.0));
      if (zhuanshu < 4 || jieduan < 4) {
        return damage; // 未达门槛，静默跳过
      }
    } catch (Throwable ignored) {}

    // 创建 Shockfield 波源
    Vec3 hitPosition = target.position();
    long currentTick = attacker.level().getGameTime();
    double amplitude = ShockfieldMath.A0_PLAYER;

    ShockfieldManager
        .getInstance()
        .create(attacker, hitPosition, amplitude, currentTick, target.getUUID(), cc, organ, STATE_ROOT);

    return damage;
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!(player.level() instanceof ServerLevel serverLevel) || player.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    long now = player.level().getGameTime();

    // 资源消耗：每秒扣除念头/精力
    if (now % 20 == 0) { // 每秒执行一次
      OptionalDouble niantou = tryConsumeNiantou(player, ShockfieldMath.COST_NIANTOU_PER_SEC);
      OptionalDouble jingli = tryConsumeJingli(player, ShockfieldMath.COST_JINGLI_PER_SEC);

      // 任一资源不足，熄灭所有波源
      if (niantou.isEmpty() || jingli.isEmpty()) {
        ShockfieldManager.getInstance().removeByOwner(player.getUUID());
        return;
      }
    }

    // 驱动已改为统一 ServerTick 引擎，这里不再触发 tickAll
  }

  // ==================== 辅助方法 ====================

  /**
   * 尝试消耗念头。
   *
   * @param player 玩家
   * @param amount 消耗量
   * @return 消耗结果
   */
  private OptionalDouble tryConsumeNiantou(Player player, double amount) {
    var handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return handleOpt.get().adjustNiantou(-amount, true);
  }

  /**
   * 尝试消耗精力。
   *
   * @param player 玩家
   * @param amount 消耗量
   * @return 消耗结果
   */
  private OptionalDouble tryConsumeJingli(Player player, double amount) {
    return ResourceOps.tryConsumeScaledJingli(player, amount);
  }

  /**
   * 查找器官ItemStack。
   *
   * @param cc 腔体实例
   * @return 器官ItemStack，未找到则返回EMPTY
   */
  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
