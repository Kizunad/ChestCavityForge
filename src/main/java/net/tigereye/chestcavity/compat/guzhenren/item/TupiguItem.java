package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;

/**
 * 土脾蛊：站地时恢复精力并给予短暂的跳跃增益。
 */
public class TupiguItem extends Item implements OrganOnGroundListener {

    private static final double JINGLI_REGEN_PER_TICK = 0.2;
    private static final int JUMP_EFFECT_TICKS = 10; // 0.5s

    public TupiguItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            double regen = JINGLI_REGEN_PER_TICK * Math.max(1, organ.getCount());
            handle.adjustJingli(regen, false);
        });

        if (!player.hasEffect(MobEffects.JUMP) || player.getEffect(MobEffects.JUMP).getDuration() <= JUMP_EFFECT_TICKS / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_EFFECT_TICKS, 0, false, false, true));
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1f, 1.2f);
        }
    }
}

