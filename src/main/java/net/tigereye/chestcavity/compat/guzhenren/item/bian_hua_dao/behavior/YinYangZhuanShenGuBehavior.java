package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuPassive} and {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuActive} instead.
 */
@Deprecated
public final class YinYangZhuanShenGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener {

    public static final YinYangZhuanShenGuBehavior INSTANCE = new YinYangZhuanShenGuBehavior();

    public void activateBody(ServerPlayer player, ChestCavityInstance cc) {
        net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuActive.INSTANCE.activateAbility(player, cc);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    }

    @Override
    public float onIncomingDamage(
        DamageSource source,
        LivingEntity entity,
        ChestCavityInstance cc,
        ItemStack organ,
        float damage) {
        return damage;
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
}
