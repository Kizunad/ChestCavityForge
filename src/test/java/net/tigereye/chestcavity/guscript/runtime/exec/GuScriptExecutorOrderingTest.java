package net.tigereye.chestcavity.guscript.runtime.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.HashMultiset;
import java.util.List;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuScriptExecutorOrderingTest {

  @BeforeEach
  void setUpConfig() {
    ChestCavity.config = new CCConfig();
  }

  @AfterEach
  void tearDownConfig() {
    ChestCavity.config = null;
  }

  @Test
  void sortRoots_prefersEarlierPageBeforeSlot() {
    LeafGuNode page0Slot5 = new LeafGuNode("page0_slot5", HashMultiset.create(), List.of(), 0, 5);
    LeafGuNode page1Slot0 = new LeafGuNode("page1_slot0", HashMultiset.create(), List.of(), 1, 0);

    List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(page1Slot0, page0Slot5));

    assertEquals(List.of(page0Slot5, page1Slot0), sorted);
  }

  @Test
  void sortRoots_ordersBySlotWithinSamePage() {
    LeafGuNode slot2 = new LeafGuNode("slot2", HashMultiset.create(), List.of(), 0, 2);
    LeafGuNode slot7 = new LeafGuNode("slot7", HashMultiset.create(), List.of(), 0, 7);

    List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(slot7, slot2));

    assertEquals(List.of(slot2, slot7), sorted);
  }

  @Test
  void sortRoots_fallsBackToOriginalOrderWhenUiPreferenceDisabled() {
    CCConfig config = new CCConfig();
    config.GUSCRIPT_EXECUTION.preferUiOrder = false;
    ChestCavity.config = config;

    LeafGuNode first = new LeafGuNode("first", HashMultiset.create(), List.of(), 0, 5);
    LeafGuNode second = new LeafGuNode("second", HashMultiset.create(), List.of(), 0, 0);

    List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(first, second));

    assertEquals(List.of(first, second), sorted);
  }
}
