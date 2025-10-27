package net.tigereye.chestcavity.compat.guzhenren.item.nudao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 御军蛊占位事件处理器：当前版本保持空实现，仅保留注册钩子以便后续演进时补充行为。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class YuJunGuHandlers {

  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_jun_gu");

  private YuJunGuHandlers() {}

  @SubscribeEvent
  public static void onIncomingDamage(LivingIncomingDamageEvent event) {
    // no-op: 御军蛊暂无实际效果
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    // no-op: 御军蛊暂无实际效果
  }
}
