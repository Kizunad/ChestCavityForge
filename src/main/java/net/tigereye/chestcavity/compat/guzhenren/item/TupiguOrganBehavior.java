package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;

/**
 * 土脾蛊：站地时恢复精力并给予短暂跳跃增益。
 */
public enum TupiguOrganBehavior implements OrganOnGroundListener {
    INSTANCE;

    private static final double JINGLI_PER_TICK = 1.0;
    private static final int JUMP_EFFECT_TICKS = 10; // 0.5s 每 tick 20ms
    private static final int JUMP_AMPLIFIER = 0;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        GuzhenrenResourceBridge.open(player).ifPresent(handle ->
                handle.adjustJingli(JINGLI_PER_TICK * Math.max(1, organ.getCount()), true));

        MobEffectInstance current = player.getEffect(MobEffects.JUMP);
        if (current == null || current.getDuration() <= JUMP_EFFECT_TICKS / 2) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, JUMP_EFFECT_TICKS, JUMP_AMPLIFIER, false, false, true));
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1f, 1.1f);
        }
    }
}
