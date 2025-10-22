package net.tigereye.chestcavity.compat.guzhenren.event;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

public final class NoDropEvents {
  private static final Logger LOG = LogUtils.getLogger();

  // ✅ 统一使用下划线标签；同时兼容你之前的冒号老标签
  public static final String TAG          = "chestcavity_no_drop";
  public static final String LEGACY_TAG   = "chestcavity:no_drop";
  public static final String PDC          = "chestcavity_no_drop";

  private static boolean REGISTERED = false;
  private NoDropEvents() {}

  public static void init() {
    if (REGISTERED) return;
    REGISTERED = true;
    // ✅ 用最高优先级并直接取消事件，防止其他模组后续再往列表里加掉落
    NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, NoDropEvents::onLivingDrops);
    NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, NoDropEvents::onLivingXP);
    LOG.info("[ChestCavity] NoDropEvents registered.");
  }

  private static boolean isNoDrop(LivingEntity e) {
    return e.getPersistentData().getBoolean(PDC)
        || e.getTags().contains(TAG)
        || e.getTags().contains(LEGACY_TAG);
  }

  private static void onLivingDrops(LivingDropsEvent e) {
    if (!isNoDrop(e.getEntity())) return;
    e.getDrops().clear();
    e.setCanceled(true); // ✅ 彻底阻断默认掉落流程及后续追加
  }

  private static void onLivingXP(LivingExperienceDropEvent e) {
    if (!isNoDrop(e.getEntity())) return;
    e.setDroppedExperience(0);
  }
}
