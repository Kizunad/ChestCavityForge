package net.tigereye.chestcavity.guscript.runtime.exec;

import com.google.common.collect.HashMultiset;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuScriptExecutionOrderTest {

    @BeforeEach
    void configure() {
        ChestCavity.config = new CCConfig();
        ChestCavity.config.GUSCRIPT_EXECUTION.preferUiOrder = true;
    }

    @AfterEach
    void tearDown() {
        ChestCavity.config = null;
    }

    @Test
    void sortRootsHonoursPrimarySlotIndex() {
        GuNode late = operator("late", 5, 2, 20);
        GuNode early = operator("early", 0, 1, 10);
        GuNode middle = operator("middle", 3, 1, 15);

        List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(late, middle, early));

        assertEquals(List.of(early, middle, late), sorted,
                "Roots should be ordered by ascending primary slot index when preference is enabled");
    }

    @Test
    void legacyOrderRespectedWhenPreferenceDisabled() {
        ChestCavity.config.GUSCRIPT_EXECUTION.preferUiOrder = false;
        GuNode first = operator("first", 5, 2, 5);
        GuNode second = operator("second", 0, 2, 15);
        GuNode third = operator("third", 3, 2, 25);

        List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(second, third, first));

        assertEquals(List.of(first, second, third), sorted,
                "When UI ordering is disabled the execution order should drive the comparator");
    }

    private static OperatorGuNode operator(String name, int primarySlot, int adjacency, int executionOrder) {
        OperatorGuNode base = new OperatorGuNode(
                "test:" + name,
                name,
                GuNodeKind.OPERATOR,
                HashMultiset.create(),
                List.of(),
                List.of(),
                executionOrder,
                false,
                false
        );
        return base.withOrderingMetadata(primarySlot, adjacency, 0, 0);
    }
}
