package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.UpkeepCheckContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 护幕飞剑事件钩子
 *
 * <p>职责：
 *
 * <ul>
 *   <li>跳过护幕飞剑的维持消耗（护幕飞剑由剑幕蛊器官提供能量维持）
 *   <li>护幕飞剑的资源消耗由剑幕蛊器官的 onSlowTick 统一管理
 * </ul>
 *
 * <p>设计原则：
 *
 * <ul>
 *   <li>护幕飞剑通过 {@link FlyingSwordEntity#isWardSword()} 标识
 *   <li>所有护幕飞剑跳过 UpkeepSystem 的真元消耗
 *   <li>资源消耗改为由剑幕蛊器官统一管理（精力 + 真元）
 * </ul>
 *
 * <p>注意：此钩子应该在模组初始化时注册到 {@link FlyingSwordEventRegistry}
 */
public final class WardSwordEventHook implements FlyingSwordEventHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(WardSwordEventHook.class);

  @Override
  public void onUpkeepCheck(UpkeepCheckContext ctx) {
    // 检查是否为护幕飞剑
    if (ctx.sword.isWardSword()) {
      // 护幕飞剑跳过维持消耗
      // 资源消耗由剑幕蛊器官的 onSlowTick 统一管理
      ctx.skipConsumption = true;

      LOGGER.debug(
          "[WardSword] Skipped upkeep consumption for ward sword (ID: {})", ctx.sword.getId());
    }
  }
}
