package net.tigereye.chestcavity.listeners;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public class OrganFoodEffectCallback { // todo: event so other mods can add??
  public static void addFoodEffects(
      List<Pair<MobEffectInstance, Float>> list,
      ItemStack itemStack,
      Level world,
      LivingEntity entity,
      ChestCavityInstance cc) {
    OrganFoodEffectListeners.callMethods(list, itemStack, world, entity, cc);
  }
}
