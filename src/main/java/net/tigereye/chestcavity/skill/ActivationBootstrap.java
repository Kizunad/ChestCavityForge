package net.tigereye.chestcavity.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 延迟加载技能行为的引导表。
 *
 * <p>部分技能（尤其是组合杀招）只在真正触发时才需要加载对应行为类。为避免在 {@link
 * net.tigereye.chestcavity.listeners.OrganActivationListeners} 中维护硬编码列表，本表负责记录“技能 ID →
 * 懒加载逻辑”的映射，供激活监听器在首次触发失败时回调。</p>
 */
public final class ActivationBootstrap {

  private static final Map<ResourceLocation, Runnable> LOADERS = new ConcurrentHashMap<>();

  private ActivationBootstrap() {}

  /** 为指定技能登记懒加载操作。后登记的会覆盖先前条目。 */
  public static void register(ResourceLocation id, Runnable loader) {
    if (id == null || loader == null) {
      return;
    }
    LOADERS.put(id, loader);
  }

  /**
   * 确保对应技能已加载。
   *
   * @return 如果存在并执行了懒加载逻辑返回 true；若无登记或执行失败则返回 false。
   */
  public static boolean ensureLoaded(ResourceLocation id) {
    if (id == null) {
      return false;
    }
    Runnable loader = LOADERS.get(id);
    if (loader == null) {
      return false;
    }
    try {
      loader.run();
    } catch (Throwable t) {
      ChestCavity.LOGGER.warn("[skill][bootstrap] failed to load ability {}: {}", id, t.toString());
      return false;
    }
    return true;
  }
}
