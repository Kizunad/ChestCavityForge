package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.hooks;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.TickContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianSuoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianSuoCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.jian_suo.JianSuoRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianSuoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑梭蛊飞剑自动突进钩子。
 *
 * <p>功能：
 *
 * <ul>
 *   <li>检测主人是否拥有剑梭蛊器官
 *   <li>定时沿飞剑朝向执行短突进
 *   <li>不消耗资源，独立冷却
 *   <li>仅对非友方有效
 * </ul>
 */
public final class JianSuoGuSwordAutoDashHook implements FlyingSwordEventHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianSuoGuSwordAutoDashHook.class);

  /** 飞剑上次突进时间记录：swordId -> lastDashTick */
  private static final ConcurrentHashMap<Integer, Long> LAST_DASH_TICK = new ConcurrentHashMap<>();

  @Override
  public void onTick(TickContext ctx) {
    if (ctx == null || ctx.sword == null || ctx.level == null || ctx.owner == null) {
      return;
    }

    // 仅服务端处理
    if (ctx.level.isClientSide()) {
      return;
    }

    // 1. 检查主人是否拥有剑梭蛊器官
    if (!ownerHasOrgan(ctx.owner)) {
      return;
    }

    // 2. 检查冷却
    long now = ctx.level.getGameTime();
    int swordId = ctx.sword.getId();
    long lastDash = LAST_DASH_TICK.getOrDefault(swordId, 0L);
    int dashInterval = JianSuoCalc.secondsToTicks(JianSuoGuTuning.SWORD_DASH_INTERVAL_S);

    if (now - lastDash < dashInterval) {
      return; // 冷却中
    }

    // 3. 读取主人的道痕（实时值）
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(ctx.owner);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double daohen = handle.read("jiandao:daohen_jiandao").orElse(0.0);

    // 4. 计算突进参数
    double dashDist = JianSuoCalc.dashDistance(daohen) * JianSuoGuTuning.SWORD_DASH_DISTANCE_SCALE;
    double velocity = ctx.sword.getDeltaMovement().length();
    double damage =
        JianSuoCalc.pathDamage(daohen, velocity) * JianSuoGuTuning.SWORD_DASH_DAMAGE_SCALE;

    // 5. 决定方向
    Vec3 dir = determineDashDirection(ctx);
    if (dir.lengthSqr() < 0.01) {
      return; // 无效方向
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianSuoGuSwordDash] Sword {} dashing: daohen={}, dist={}, dmg={}",
          swordId,
          daohen,
          dashDist,
          damage);
    }

    // 6. 执行突进（使用主人作为攻击者进行敌我判断）
    double actualDist =
        JianSuoRuntime.tryDashAndDamage(
            ctx.sword,
            ctx.owner, // 主人作为攻击者，用于敌我判断和伤害源
            dir,
            dashDist,
            damage,
            JianSuoGuTuning.RAY_WIDTH,
            6, // 较少的步数（飞剑版简化）
            JianSuoGuTuning.HIT_ONCE_DEDUP_TICKS);

    // 7. 记录时间
    LAST_DASH_TICK.put(swordId, now);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[JianSuoGuSwordDash] Actual distance: {}", actualDist);
    }
  }

  @Override
  public void onDespawnOrRecall(
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.DespawnContext ctx) {
    // 清理记录
    if (ctx != null && ctx.sword != null) {
      LAST_DASH_TICK.remove(ctx.sword.getId());
    }
  }

  /** 检查主人是否拥有剑梭蛊器官。 */
  private boolean ownerHasOrgan(LivingEntity owner) {
    if (owner == null) {
      return false;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);

    if (cc == null || cc.inventory == null) {
      return false;
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (itemId != null
          && (itemId.equals(JianSuoGuOrganBehavior.ORGAN_ID_3)
              || itemId.equals(JianSuoGuOrganBehavior.ORGAN_ID_4)
              || itemId.equals(JianSuoGuOrganBehavior.ORGAN_ID_5))) {
        return true;
      }
    }

    return false;
  }

  /** 决定突进方向（优先目标，否则视线方向）。 */
  private Vec3 determineDashDirection(TickContext ctx) {
    // 优先朝向目标
    LivingEntity target = ctx.sword.getTargetEntity();
    if (target != null) {
      Vec3 toTarget = target.position().subtract(ctx.sword.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0, toTarget.z);
      return horizontal.lengthSqr() > 0.01 ? horizontal.normalize() : toTarget.normalize();
    }

    // 否则取视线方向（水平）
    Vec3 lookAngle = ctx.sword.getViewVector(1.0F);
    Vec3 horizontal = new Vec3(lookAngle.x, 0, lookAngle.z);
    return horizontal.lengthSqr() > 0.01 ? horizontal.normalize() : lookAngle.normalize();
  }
}
