package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BingXueDaohenOpsTest {

    @Test
    public void testCalculateDaohen() {
        // Test with 3 stacks and an increase of 0.15 per stack
        assertEquals(0.45, BingXueDaohenOps.calculateDaohen(3, 0.15), 0.001);

        // Test with 0 stacks
        assertEquals(0.0, BingXueDaohenOps.calculateDaohen(0, 0.15), 0.001);

        // Test with a negative increase per stack
        assertEquals(-0.45, BingXueDaohenOps.calculateDaohen(3, -0.15), 0.001);
    }
}
