package net.tigereye.chestcavity.compat.guzhenren.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;

import javax.annotation.Nullable;

/**
 * FX helpers for 小光蛊。抽离出粒子/音效触发，避免行为类塞入重复逻辑。
 */
public final class XiaoGuangFx {

    private XiaoGuangFx() {
    }

    private static final ResourceLocation MIRROR_TRIGGER = ResourceLocation.parse("chestcavity:xiao_guang_mirror_trigger");
    private static final ResourceLocation MIRROR_EVADE = ResourceLocation.parse("chestcavity:xiao_guang_mirror_evade");
    private static final ResourceLocation ILLUSION_SUMMON = ResourceLocation.parse("chestcavity:xiao_guang_illusion_summon");
    private static final ResourceLocation ILLUSION_BURST = ResourceLocation.parse("chestcavity:xiao_guang_illusion_burst");
    private static final ResourceLocation LIGHTSTEP = ResourceLocation.parse("chestcavity:xiao_guang_lightstep");

    public static void playMirrorTrigger(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        Vec3 origin = entity.position().add(0.0D, entity.getBbHeight() * 0.4D, 0.0D);
        Vec3 look = entity.getLookAngle();
        dispatch(entity, MIRROR_TRIGGER, origin, look, entity);
    }

    public static void playMirrorEvade(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        Vec3 origin = entity.position();
        Vec3 look = entity.getLookAngle();
        dispatch(entity, MIRROR_EVADE, origin, look, entity);
    }

    public static void playLightstep(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        Vec3 origin = entity.position();
        Vec3 look = entity.getLookAngle();
        dispatch(entity, LIGHTSTEP, origin, look, entity);
    }

    public static void playIllusionSummon(@Nullable LivingEntity owner, LivingEntity decoy) {
        if (decoy == null) {
            return;
        }
        Vec3 origin = decoy.position();
        Vec3 look = decoy.getLookAngle();
        ServerPlayer viewer = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (decoy.level() instanceof ServerLevel level) {
            AbilityFxDispatcher.play(level, ILLUSION_SUMMON, origin, look, look, viewer, decoy, 1.0F);
        }
    }

    public static void playIllusionBurst(ServerLevel level, Vec3 origin) {
        if (level == null || origin == null) {
            return;
        }
        AbilityFxDispatcher.play(level, ILLUSION_BURST, origin, Vec3.ZERO, Vec3.ZERO, null, null, 1.0F);
    }

    private static void dispatch(LivingEntity entity, ResourceLocation id, Vec3 origin, Vec3 forward, @Nullable LivingEntity focus) {
        if (entity instanceof ServerPlayer serverPlayer) {
            AbilityFxDispatcher.play(serverPlayer, id, Vec3.ZERO, forward, 1.0F);
            return;
        }
        if (entity.level() instanceof ServerLevel level) {
            AbilityFxDispatcher.play(level, id, origin, forward, forward,
                    entity instanceof ServerPlayer sp ? sp : null, focus, 1.0F);
        }
    }
}
