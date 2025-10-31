package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/** 鱼鳞蛊 Organ 行为（只做 slowTick 转发到 Calculator）。 */
public enum YuLinGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
      YuLinGuOps.tickSummons(level, player, player.level().getGameTime());
    }
  }
}
