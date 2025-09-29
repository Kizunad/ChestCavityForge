package net.tigereye.chestcavity.guscript.network.packets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuScriptTriggerPayloadTest {

    @Test
    void isTargetAllowedRejectsWhenOutOfRange() {
        assertFalse(GuScriptTriggerPayload.isTargetAllowed(GuScriptTriggerPayload.MAX_TARGET_RANGE_SQR + 1.0, true, 1.0));
    }

    @Test
    void isTargetAllowedRejectsWithoutLineOfSight() {
        assertFalse(GuScriptTriggerPayload.isTargetAllowed(25.0, false, 1.0));
    }

    @Test
    void isTargetAllowedRejectsOutsideViewCone() {
        assertFalse(GuScriptTriggerPayload.isTargetAllowed(25.0, true, GuScriptTriggerPayload.MIN_VIEW_DOT - 0.01));
    }

    @Test
    void isTargetAllowedAcceptsValidParameters() {
        assertTrue(GuScriptTriggerPayload.isTargetAllowed(25.0, true, GuScriptTriggerPayload.MIN_VIEW_DOT + 0.01));
    }
}

