package net.tigereye.chestcavity.compat.guzhenren.event;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * 服务端事件监听：为打上特殊标签的召唤物清空掉落与经验。
 *
 * <p>设计目标：集中处理“无掉落召唤物”，避免在各召唤点分散覆写。
 */
public final class NoDropEvents {

  /** 公共标签常量，召唤逻辑通过实体标签判定。 */
  public static final String TAG = "chestcavity:no_drop";

  /** 公共 PDC 键名，供无法持久存标签的场景使用。 */
  public static final String PDC = "chestcavity_no_drop";

  private NoDropEvents() {}

  /** 初始化事件监听。显式注册到全局事件总线，避免依赖注解扫描导致的加载顺序问题。 */
  public static void init() {
    NeoForge.EVENT_BUS.addListener(NoDropEvents::onLivingDrops);
    NeoForge.EVENT_BUS.addListener(NoDropEvents::onLivingXP);
  }

  /**
   * 判定实体是否被标记为“无掉落”。
   *
   * <p>兼容实体标签与 PDC，确保跨重载与持久化场景均能命中。
   */
  private static boolean isNoDrop(LivingEntity entity) {
    return entity.getTags().contains(TAG) || entity.getPersistentData().getBoolean(PDC);
  }

  /** 清除被标记召唤物的所有掉落物。 */
  private static void onLivingDrops(LivingDropsEvent event) {
    if (isNoDrop(event.getEntity())) {
      event.getDrops().clear();
    }
  }

  /** 拦截经验掉落，阻止被标记召唤物提供经验值。 */
  private static void onLivingXP(LivingExperienceDropEvent event) {
    if (isNoDrop(event.getEntity())) {
      event.setDroppedExperience(0);
    }
  }
}
