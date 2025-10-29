package net.tigereye.chestcavity.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkillActivationHooksTest {

  private static final ResourceLocation ACTIVE_SKILL_ID =
      new ResourceLocation("guzhenren", "test_active");
  private static final ResourceLocation COMBO_SKILL_ID =
      new ResourceLocation("guzhenren", "test_combo");

  private static ActiveSkillRegistry.ActiveSkillEntry dummyActiveEntry(ResourceLocation id) {
    return new ActiveSkillRegistry.ActiveSkillEntry(
        id, id, id, List.of(), "desc", "src", ActiveSkillRegistry.CooldownHint.useOrgan("t", null));
  }

  private static ComboSkillRegistry.ComboSkillEntry dummyComboEntry(ResourceLocation id) {
    return new ComboSkillRegistry.ComboSkillEntry(
        id,
        "display",
        id,
        List.of(new ResourceLocation("guzhenren", "organ")),
        List.of(),
        List.of(),
        "cat",
        "subcat",
        "desc",
        List.of(),
        "source",
        ActiveSkillRegistry.CooldownHint.useOrgan("t", null));
  }

  @BeforeEach
  void resetHooks() {
    SkillActivationHooks.clearAllHandlers();
  }

  @Test
  void activePreHandlerMatchesRegexAndContinues() {
    AtomicInteger matched = new AtomicInteger();
    AtomicInteger unmatched = new AtomicInteger();

    SkillActivationHooks.registerActivePreHandler(
        "^guzhenren:.*$",
        (player, skillId, cc, entry) -> {
          matched.incrementAndGet();
          return SkillActivationHooks.ActivePreHookDecision.continueChain();
        });

    SkillActivationHooks.registerActivePreHandler(
        "^other:.*$",
        (player, skillId, cc, entry) -> {
          unmatched.incrementAndGet();
          return SkillActivationHooks.ActivePreHookDecision.continueChain();
        });

    SkillActivationHooks.ActivePreHookDecision decision =
        SkillActivationHooks.fireActivePreHandlers(
            null, ACTIVE_SKILL_ID, null, dummyActiveEntry(ACTIVE_SKILL_ID));

    assertTrue(decision.shouldProceed());
    assertEquals(1, matched.get(), "匹配的处理器应被调用一次");
    assertEquals(0, unmatched.get(), "不匹配的处理器不应触发");
  }

  @Test
  void activePreHandlerBlocksAndSkipsFollowing() {
    AtomicInteger invoked = new AtomicInteger();

    SkillActivationHooks.registerActivePreHandler(
        "^guzhenren:.*$",
        (player, skillId, cc, entry) -> {
          invoked.incrementAndGet();
          return SkillActivationHooks.ActivePreHookDecision.cancel(
              ActiveSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER);
        });

    SkillActivationHooks.registerActivePreHandler(
        "^guzhenren:.*$",
        (player, skillId, cc, entry) -> {
          invoked.incrementAndGet();
          return SkillActivationHooks.ActivePreHookDecision.continueChain();
        });

    SkillActivationHooks.ActivePreHookDecision decision =
        SkillActivationHooks.fireActivePreHandlers(
            null, ACTIVE_SKILL_ID, null, dummyActiveEntry(ACTIVE_SKILL_ID));

    assertFalse(decision.shouldProceed());
    assertEquals(
        ActiveSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER, decision.overrideResult());
    assertEquals(1, invoked.get(), "后续处理器应在被拦截后跳过");
  }

  @Test
  void comboPostHandlersRespectRegex() {
    AtomicReference<ComboSkillRegistry.TriggerResult> matched = new AtomicReference<>();
    AtomicInteger catchAll = new AtomicInteger();

    SkillActivationHooks.registerComboPostHandler(
        "^guzhenren:test_combo$",
        (player, skillId, cc, entry, result) -> matched.set(result));

    SkillActivationHooks.registerComboPostHandler(
        null,
        (player, skillId, cc, entry, result) -> catchAll.incrementAndGet());

    SkillActivationHooks.fireComboPostHandlers(
        null,
        COMBO_SKILL_ID,
        null,
        dummyComboEntry(COMBO_SKILL_ID),
        ComboSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER);

    assertEquals(
        ComboSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER,
        matched.get(),
        "匹配的组合后置处理器应收集结果");
    assertEquals(1, catchAll.get(), "兜底处理器应当被调用一次");
  }

  @Test
  void activePostHandlersTriggered() {
    AtomicInteger matched = new AtomicInteger();

    SkillActivationHooks.registerActivePostHandler(
        "^guzhenren:test_active$",
        (player, skillId, cc, entry, result) -> matched.incrementAndGet());

    SkillActivationHooks.fireActivePostHandlers(
        null,
        ACTIVE_SKILL_ID,
        null,
        dummyActiveEntry(ACTIVE_SKILL_ID),
        ActiveSkillRegistry.TriggerResult.SUCCESS);

    assertEquals(1, matched.get(), "主动技能后置处理器应在触发后调用");
  }
}
