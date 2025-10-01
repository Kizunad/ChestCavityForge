package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeckoFxRelativeOffsetRotationTest {

    @Test
    void yawRotationMatchesMinecraftFacing() throws Exception {
        Method rotate = FlowActions.class.getDeclaredMethod("rotateRelativeOffset", Vec3.class, float.class, float.class, float.class);
        rotate.setAccessible(true);
        Vec3 forward = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 west = (Vec3) rotate.invoke(null, forward, -90.0F, 0.0F, 0.0F);
        assertEquals(-1.0D, west.x, 1.0E-6D);
        assertEquals(0.0D, west.y, 1.0E-6D);
        assertEquals(0.0D, west.z, 1.0E-6D);
    }

    @Test
    void zeroRotationReturnsOriginalVector() throws Exception {
        Method rotate = FlowActions.class.getDeclaredMethod("rotateRelativeOffset", Vec3.class, float.class, float.class, float.class);
        rotate.setAccessible(true);
        Vec3 offset = new Vec3(1.25D, -0.5D, 3.0D);
        Vec3 rotated = (Vec3) rotate.invoke(null, offset, 0.0F, 0.0F, 0.0F);
        assertEquals(offset.x, rotated.x, 1.0E-6D);
        assertEquals(offset.y, rotated.y, 1.0E-6D);
        assertEquals(offset.z, rotated.z, 1.0E-6D);
    }
}
