package net.tigereye.chestcavity.mob_effect.fx;


import net.minecraft.util.SoundEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FurnacePowerFxHelperTest {

    @Test
    void chargingStateReturnsCrackleSound() {
        assertEquals(SoundEvents.FURNACE_FIRE_CRACKLE,
                FurnacePowerFxHelper.soundFor(FurnaceFlowState.CHARGING).orElse(null));
    }

    @Test
    void feedingStateReturnsBurpSound() {
        assertEquals(SoundEvents.PLAYER_BURP,
                FurnacePowerFxHelper.soundFor(FurnaceFlowState.FEEDING).orElse(null));
    }

    @Test
    void idleStateReturnsNoSound() {
        assertTrue(FurnacePowerFxHelper.soundFor(FurnaceFlowState.IDLE).isEmpty());
    }
}
