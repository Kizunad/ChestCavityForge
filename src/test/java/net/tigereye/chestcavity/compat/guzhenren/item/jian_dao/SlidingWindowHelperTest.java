package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.SwordShadowRuntime;
import org.junit.jupiter.api.Test;

public class SlidingWindowHelperTest {

  @Test
  void windowAcceptAndRecord_respectsCapacityAndCooldown() {
    ArrayDeque<Long> history = new ArrayDeque<>();
    int capacity = 2;
    int cd = 100;

    // first accept at t=100
    assertTrue(SwordShadowRuntime.windowAcceptAndRecord(history, 100L, capacity, cd));
    assertEquals(1, history.size());

    // second accept at t=150 (within cd but capacity allows 2 concurrent)
    assertTrue(SwordShadowRuntime.windowAcceptAndRecord(history, 150L, capacity, cd));
    assertEquals(2, history.size());

    // third attempt at t=180 rejected due to capacity reached within window
    assertFalse(SwordShadowRuntime.windowAcceptAndRecord(history, 180L, capacity, cd));
    assertEquals(2, history.size());

    // after window passes relative to first (t=210), one slot should free and accept
    assertTrue(SwordShadowRuntime.windowAcceptAndRecord(history, 210L, capacity, cd));
    assertEquals(2, history.size());
  }

  @Test
  void windowAcceptAndRecord_handlesTimeSkew() {
    ArrayDeque<Long> history = new ArrayDeque<>();
    int capacity = 1;
    int cd = 50;

    assertTrue(SwordShadowRuntime.windowAcceptAndRecord(history, 100L, capacity, cd));
    assertEquals(1, history.size());

    // time skew backwards: now < head; should clear and accept
    assertTrue(SwordShadowRuntime.windowAcceptAndRecord(history, 90L, capacity, cd));
    assertEquals(1, history.size());
    assertEquals(90L, history.peekFirst());
  }
}

