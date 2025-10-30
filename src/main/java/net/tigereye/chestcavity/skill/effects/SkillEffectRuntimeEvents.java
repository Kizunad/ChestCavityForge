package net.tigereye.chestcavity.skill.effects;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 运行期事件：在玩家离线/死亡/换维度时清理挂起的技能临时状态，防止效果残留。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SkillEffectRuntimeEvents {

  private SkillEffectRuntimeEvents() {}

  @SubscribeEvent
  public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      SkillEffectBus.cleanup(player);
    }
  }

  @SubscribeEvent
  public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      SkillEffectBus.cleanup(player);
    }
  }

  @SubscribeEvent
  public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      SkillEffectBus.cleanup(player);
    }
  }

  @SubscribeEvent
  public static void onDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      SkillEffectBus.cleanup(player);
    }
  }
}

