package net.tigereye.chestcavity.listeners;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.ArrayList;
import java.util.List;

public class OrganFoodEffectCallback { //todo: event so other mods can add??
    public static void addFoodEffects(List<Pair<MobEffectInstance, Float>> list, ItemStack itemStack, Level world, LivingEntity entity, ChestCavityInstance cc) {
        OrganFoodEffectListeners.callMethods(list, itemStack, world, entity, cc);
    }
}

