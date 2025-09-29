package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuScriptTriggerPayloadTest {

    @Test
    void resolveTargetRejectsSpoofBeyondRange() {
        var context = new TargetingContext();
        context.distanceSquared = 512.0;
        var result = context.invokeResolve();
        assertNull(result);
    }

    @Test
    void resolveTargetRejectsWhenOutsideViewCone() {
        var context = new TargetingContext();
        context.lookVector = new Vec3(1.0, 0.0, 0.0);
        context.targetEye = new Vec3(0.0, 0.0, 5.0);
        var result = context.invokeResolve();
        assertNull(result);
    }

    @Test
    void resolveTargetRejectsWithoutLineOfSight() {
        var context = new TargetingContext();
        context.hasLineOfSight = false;
        var result = context.invokeResolve();
        assertNull(result);
    }

    @Test
    void resolveTargetAcceptsValidTargetInFront() {
        var context = new TargetingContext();
        var result = context.invokeResolve();
        assertSame(context.target, result);
    }

    @Test
    void resolveTargetAllowsSelfEntity() {
        var player = Mockito.mock(ServerPlayer.class, Answers.RETURNS_DEEP_STUBS);
        var level = Mockito.mock(ServerLevel.class, Answers.RETURNS_DEEP_STUBS);
        Mockito.when(player.level()).thenReturn(level);
        Mockito.when(level.getEntity(7)).thenReturn(player);
        Mockito.when(player.isAlive()).thenReturn(true);
        Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
        var result = GuScriptTriggerPayload.resolveTarget(player, 7);
        assertSame(player, result);
    }

    private static final class TargetingContext {
        final ServerPlayer player = Mockito.mock(ServerPlayer.class, Answers.RETURNS_DEEP_STUBS);
        final ServerLevel level = Mockito.mock(ServerLevel.class, Answers.RETURNS_DEEP_STUBS);
        final LivingEntity target = Mockito.mock(LivingEntity.class, Answers.RETURNS_DEEP_STUBS);
        double distanceSquared = 100.0;
        Vec3 lookVector = new Vec3(0.0, 0.0, 1.0);
        Vec3 playerEye = Vec3.ZERO;
        Vec3 targetEye = new Vec3(0.0, 0.0, 10.0);
        boolean hasLineOfSight = true;

        TargetingContext() {
            Mockito.when(player.level()).thenReturn(level);
            Mockito.when(level.getEntity(42)).thenReturn(target);
            Mockito.when(target.isAlive()).thenReturn(true);
            Mockito.when(target.getUUID()).thenReturn(UUID.randomUUID());
            Mockito.when(player.getUUID()).thenReturn(UUID.randomUUID());
            Mockito.when(player.distanceToSqr(target)).thenAnswer(invocation -> distanceSquared);
            Mockito.when(player.hasLineOfSight(target)).thenAnswer(invocation -> hasLineOfSight);
            Mockito.when(player.getEyePosition()).thenAnswer(invocation -> playerEye);
            Mockito.when(player.getLookAngle()).thenAnswer(invocation -> lookVector);
            Mockito.when(target.getEyePosition()).thenAnswer(invocation -> targetEye);
        }

        LivingEntity invokeResolve() {
            return GuScriptTriggerPayload.resolveTarget(player, 42);
        }
    }
}
