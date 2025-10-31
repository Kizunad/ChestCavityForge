package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuPassive} instead.
 */
@Deprecated
public class YinYangZhuanShenGuPassive implements PassiveHook {
    @Override
    public void onTick(@NotNull LivingEntity self, @NotNull ChestCavityInstance cc, long time) {
        net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuPassive.INSTANCE.onTick(self, cc, time);
    }

    @Override
    public float onHurt(@NotNull LivingEntity self, @NotNull ChestCavityInstance cc, @NotNull DamageSource source, float amount, long time) {
        return net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.behavior.YinYangZhuanShenGuPassive.INSTANCE.onHurt(self, cc, source, amount, time);
    }
}
