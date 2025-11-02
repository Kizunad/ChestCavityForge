package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianXinGuPassiveCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianXinGuStateOps;

/**
 * “定心返本”触发事件（控制类效果添加）。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class JianXinGuPassiveEvents {

  private JianXinGuPassiveEvents() {}

  @SubscribeEvent
  public static void onEffectAdded(MobEffectEvent.Added event) {
    LivingEntity entity = event.getEntity();
    MobEffectInstance inst = event.getEffectInstance();
    if (!(entity instanceof ServerPlayer player) || inst == null) {
      return;
    }
    if (!JianXinGuPassiveCalc.isControlEffect(inst)) {
      return;
    }
    // 定位 OrganState 并累积定心值
    java.util.Optional<OrganState> st = JianXinGuStateOps.resolve(player);
    st.ifPresent(state -> JianXinGuPassiveCalc.onControlEffect(player, state));
  }
}
