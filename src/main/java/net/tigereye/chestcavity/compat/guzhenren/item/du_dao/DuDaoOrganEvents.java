package net.tigereye.chestcavity.compat.guzhenren.item.du_dao;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.behavior.ChouPiGuOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/** Event bridge for Du Dao organs. */
public final class DuDaoOrganEvents {

  private static boolean registered;

  private DuDaoOrganEvents() {}

  public static void register() {
    if (registered) {
      return;
    }
    registered = true;
    NeoForge.EVENT_BUS.addListener(DuDaoOrganEvents::onItemUseFinish);
  }

  private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }

    ItemStack consumed = event.getItem();
    if (consumed.isEmpty()) {
      return;
    }

    double triggerChance = 0.0;
    if (consumed.is(ItemTags.MEAT) || consumed.is(Items.ROTTEN_FLESH)) {
      triggerChance = ChouPiGuOrganBehavior.FOOD_TRIGGER_BASE_CHANCE;
      if (consumed.is(Items.ROTTEN_FLESH)) {
        triggerChance *= ChouPiGuOrganBehavior.ROTTEN_FOOD_MULTIPLIER;
      }
    }
    if (triggerChance <= 0.0) {
      return;
    }

    final double chance = triggerChance;
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> ChouPiGuOrganBehavior.INSTANCE.onFoodConsumed(player, cc, consumed, chance));
  }
}
