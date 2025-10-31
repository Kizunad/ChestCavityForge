package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.passive.YinYangZhuanShenGuPassive;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/** 阴阳转身蛊 Organ 行为：slowTick 桥接到被动，便于逐步迁移到 Organ 监听。 */
public enum YinYangZhuanShenGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    YinYangZhuanShenGuPassive.INSTANCE.onTick(entity, cc, entity.level().getGameTime());
  }
}

