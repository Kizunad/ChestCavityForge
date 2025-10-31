package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.behavior.YuLinGuPassive} instead.
 */
@Deprecated
public final class YuLinGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganOnHitListener,
        OrganIncomingDamageListener,
        OrganRemovalListener {

    public static final YuLinGuBehavior INSTANCE = new YuLinGuBehavior();

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    }

    @Override
    public float onHit(
        DamageSource source,
        LivingEntity attacker,
        LivingEntity target,
        ChestCavityInstance cc,
        ItemStack organ,
        float damage) {
        return damage;
    }

    @Override
    public float onIncomingDamage(
        DamageSource source,
        LivingEntity victim,
        ChestCavityInstance cc,
        ItemStack organ,
        float damage) {
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    }
}
