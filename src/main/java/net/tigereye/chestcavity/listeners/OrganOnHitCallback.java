package net.tigereye.chestcavity.listeners;


import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public class OrganOnHitCallback {
    public static void organOnHit(LivingEntity attacker, LivingEntity target, ChestCavityInstance cc) {
        OrganOnHitListeners.callMethods(attacker,target, cc);
    }
}
