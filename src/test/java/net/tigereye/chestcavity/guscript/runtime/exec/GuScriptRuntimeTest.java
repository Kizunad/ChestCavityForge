package net.tigereye.chestcavity.guscript.runtime.exec;

import com.google.common.collect.ImmutableMultiset;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.actions.AddDamageMultiplierAction;
import net.tigereye.chestcavity.guscript.actions.AddFlatDamageAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.actions.ExportFlatModifierAction;
import net.tigereye.chestcavity.guscript.actions.ExportMultiplierModifierAction;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.registry.GuScriptRegistry;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuScriptRuntimeTest {

    @BeforeEach
    void setUpRegistry() {
        Map<ResourceLocation, GuScriptRegistry.LeafDefinition> leaves = new HashMap<>();
        leaves.put(ResourceLocation.fromNamespaceAndPath("minecraft", "bone"),
                new GuScriptRegistry.LeafDefinition(
                        "骨蛊",
                        ImmutableMultiset.of("骨"),
                        List.of(new ConsumeHealthAction(2))
                ));
        leaves.put(ResourceLocation.fromNamespaceAndPath("minecraft", "nether_wart"),
                new GuScriptRegistry.LeafDefinition(
                        "血蛊",
                        ImmutableMultiset.of("血"),
                        List.of(new ConsumeZhenyuanAction(5))
                ));
        leaves.put(ResourceLocation.fromNamespaceAndPath("minecraft", "gunpowder"),
                new GuScriptRegistry.LeafDefinition(
                        "爆发蛊",
                        ImmutableMultiset.of("爆发"),
                        List.of(new AddDamageMultiplierAction(0.5))
                ));
        GuScriptRegistry.updateLeaves(leaves);

        ReactionRule core = ReactionRule.builder("chestcavity:blood_bone_core")
                .arity(2)
                .requiredTags(ImmutableMultiset.of("骨", "血"))
                .priority(50)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨核心", GuNodeKind.OPERATOR,
                        ImmutableMultiset.of("核心"),
                        List.of(new AddDamageMultiplierAction(0.5)),
                        inputs))
                .build();

        ReactionRule explosion = ReactionRule.builder("chestcavity:blood_bone_explosion")
                .arity(2)
                .requiredTags(ImmutableMultiset.of("核心", "爆发"))
                .priority(40)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨爆裂枪", GuNodeKind.COMPOSITE,
                        ImmutableMultiset.of("杀招"),
                        List.of(new EmitProjectileAction("minecraft:arrow", 6.0)),
                        inputs))
                .build();

        GuScriptRegistry.updateReactionRules(List.of(core, explosion));
    }

    @AfterEach
    void resetRegistry() {
        GuScriptRegistry.updateLeaves(Map.of());
        GuScriptRegistry.updateReactionRules(List.of());
    }

    @Test
    void executeAll_firesActionsForEveryRoot() {
        GuScriptRegistry.LeafDefinition bone = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "bone")).orElseThrow();
        GuScriptRegistry.LeafDefinition blood = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "nether_wart")).orElseThrow();
        GuScriptRegistry.LeafDefinition burst = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "gunpowder")).orElseThrow();

        List<GuNode> leaves = List.of(
                bone.toNode(), blood.toNode(), burst.toNode(),
                bone.toNode(), blood.toNode(), burst.toNode()
        );

        GuScriptReducer reducer = new GuScriptReducer();
        GuScriptReducer.ReductionResult reduced = reducer.reduce(leaves, GuScriptRegistry.reactionRules());
        List<GuNode> killMoves = reduced.roots().stream()
                .filter(node -> node.kind() == GuNodeKind.COMPOSITE)
                .toList();

        assertEquals(2, killMoves.size(), "Expected two composite kill-move roots");
        assertTrue(killMoves.stream().allMatch(node -> node.name().equals("血骨爆裂枪")));

        RecordingBridge bridge = new RecordingBridge();
        AtomicInteger contextsCreated = new AtomicInteger();
        GuScriptRuntime runtime = new GuScriptRuntime();
        runtime.executeAll(killMoves, () -> {
            contextsCreated.incrementAndGet();
            return new RecordingContext(bridge);
        });

        assertEquals(2, bridge.projectiles.get(), "Each root should emit a projectile");
        assertEquals(10, bridge.zhenyuan.get(), "Both blood gu should consume zhenyuan");
        assertEquals(4, bridge.health.get(), "Both bone gu should consume health");
        assertEquals(2, contextsCreated.get(), "Each root should create a fresh context");
    }

    @Test
    void executeAll_dispatchesEveryReducerRoot() {
        GuScriptRegistry.LeafDefinition bone = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "bone")).orElseThrow();
        GuScriptRegistry.LeafDefinition blood = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "nether_wart")).orElseThrow();
        GuScriptRegistry.LeafDefinition burst = GuScriptRegistry.leaf(ResourceLocation.fromNamespaceAndPath("minecraft", "gunpowder")).orElseThrow();

        List<GuNode> leaves = List.of(
                bone.toNode(), blood.toNode(), burst.toNode(),
                bone.toNode(), blood.toNode(), burst.toNode()
        );

        GuScriptReducer reducer = new GuScriptReducer();
        GuScriptReducer.ReductionResult reduced = reducer.reduce(leaves, GuScriptRegistry.reactionRules());

        RecordingBridge bridge = new RecordingBridge();
        AtomicInteger contextsCreated = new AtomicInteger();
        GuScriptRuntime runtime = new GuScriptRuntime();
        runtime.executeAll(reduced.roots(), () -> {
            contextsCreated.incrementAndGet();
            return new RecordingContext(bridge);
        });

        assertEquals(2, bridge.projectiles.get(), "All composite kill moves should fire projectiles");
        assertTrue(contextsCreated.get() >= 2, "Each root should request a context");
    }

    @Test
    void executionSession_exportsStackAcrossRoots() {
        ExecutionSession session = new ExecutionSession(5.0D, 50.0D);
        GuScriptRuntime runtime = new GuScriptRuntime();
        CaptureDamageAction capture = new CaptureDamageAction(10.0D);

        OperatorGuNode first = new OperatorGuNode(
                "test:first",
                "First",
                GuNodeKind.COMPOSITE,
                ImmutableMultiset.of(),
                List.of(new AddDamageMultiplierAction(0.5D)),
                List.of(),
                0,
                true,
                false
        );

        OperatorGuNode second = new OperatorGuNode(
                "test:second",
                "Second",
                GuNodeKind.COMPOSITE,
                ImmutableMultiset.of(),
                List.of(new AddDamageMultiplierAction(0.25D), capture),
                List.of(),
                1,
                false,
                false
        );

        List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(second, first));
        // expect first order 0 root before order 1 regardless of insertion order
        assertEquals(List.of(first, second), sorted);

        for (GuNode node : sorted) {
            DefaultGuScriptContext context = new DefaultGuScriptContext(null, null, new RecordingBridge(), session);
            if (node instanceof OperatorGuNode operator) {
                context.enableModifierExports(operator.exportMultiplier(), operator.exportFlat());
            }
            runtime.execute(node, context);
            session.exportMultiplier(context.exportedMultiplierDelta());
            session.exportFlat(context.exportedFlatDelta());
        }

        assertEquals(0.5D, session.currentMultiplier(), 1.0E-6, "First root exports should seed multiplier");
        assertEquals(0.0D, session.currentFlat(), 1.0E-6, "No flat exports expected");
        assertEquals(17.5D, capture.capturedDamage(), 1.0E-6, "Second root should see seeded multiplier before its own delta");
    }

    @Test
    void executionSession_ordersAndClampsExports() {
        ExecutionSession session = new ExecutionSession(0.5D, 6.0D);
        GuScriptRuntime runtime = new GuScriptRuntime();

        OperatorGuNode lateMultiplier = new OperatorGuNode(
                "test:late",
                "Late",
                GuNodeKind.COMPOSITE,
                ImmutableMultiset.of(),
                List.of(new AddDamageMultiplierAction(0.3D)),
                List.of(),
                2,
                true,
                false
        );

        OperatorGuNode earlyExports = new OperatorGuNode(
                "test:early",
                "Early",
                GuNodeKind.COMPOSITE,
                ImmutableMultiset.of(),
                List.of(new ExportMultiplierModifierAction(0.6D), new ExportFlatModifierAction(5.0D)),
                List.of(),
                0,
                false,
                false
        );

        RecordModifiersAction captureIntermediate = new RecordModifiersAction();
        OperatorGuNode middleFlat = new OperatorGuNode(
                "test:middle",
                "Middle",
                GuNodeKind.COMPOSITE,
                ImmutableMultiset.of(),
                List.of(captureIntermediate, new AddFlatDamageAction(4.0D)),
                List.of(),
                1,
                false,
                true
        );

        List<GuNode> sorted = GuScriptExecutor.sortRootsForSession(List.of(lateMultiplier, earlyExports, middleFlat));
        assertEquals(List.of(earlyExports, middleFlat, lateMultiplier), sorted, "Roots should sort by declared order");

        for (GuNode node : sorted) {
            DefaultGuScriptContext context = new DefaultGuScriptContext(null, null, new RecordingBridge(), session);
            if (node instanceof OperatorGuNode operator) {
                context.enableModifierExports(operator.exportMultiplier(), operator.exportFlat());
            }
            runtime.execute(node, context);
            double deltaMultiplier = context.exportedMultiplierDelta();
            double deltaFlat = context.exportedFlatDelta();
            if (deltaMultiplier != 0.0D) {
                session.exportMultiplier(deltaMultiplier);
            }
            if (deltaFlat != 0.0D) {
                session.exportFlat(deltaFlat);
            }
        }

        assertEquals(0.5D, session.currentMultiplier(), 1.0E-6, "Multiplier exports should clamp to cap");
        assertEquals(6.0D, session.currentFlat(), 1.0E-6, "Flat exports should clamp to cap");
        assertEquals(0.5D, captureIntermediate.multiplier(), 1.0E-6, "Middle root should see seeded multiplier from early root");
        assertEquals(5.0D, captureIntermediate.flat(), 1.0E-6, "Middle root should see seeded flat damage from early root");
    }

    private static final class RecordingBridge implements GuScriptExecutionBridge {
        final AtomicInteger zhenyuan = new AtomicInteger();
        final AtomicInteger health = new AtomicInteger();
        final AtomicInteger projectiles = new AtomicInteger();

        @Override
        public void consumeZhenyuan(int amount) {
            zhenyuan.addAndGet(amount);
        }

        @Override
        public void consumeHealth(int amount) {
            health.addAndGet(amount);
        }

        @Override
        public void emitProjectile(String projectileId, double damage) {
            projectiles.incrementAndGet();
        }
    }

    private static final class RecordingContext implements GuScriptContext {
        private final RecordingBridge bridge;
        private double multiplier;
        private double flat;

        private RecordingContext(RecordingBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public net.minecraft.world.entity.player.Player performer() {
            return null;
        }

        @Override
        public net.minecraft.world.entity.LivingEntity target() {
            return null;
        }

        @Override
        public GuScriptExecutionBridge bridge() {
            return bridge;
        }

        @Override
        public void addDamageMultiplier(double multiplier) {
            this.multiplier += multiplier;
        }

        @Override
        public void addFlatDamage(double amount) {
            this.flat += amount;
        }

        @Override
        public double damageMultiplier() {
            return multiplier;
        }

        @Override
        public double flatDamageBonus() {
            return flat;
        }
    }

    private static final class CaptureDamageAction implements net.tigereye.chestcavity.guscript.ast.Action {
        private final double baseDamage;
        private double captured;

        private CaptureDamageAction(double baseDamage) {
            this.baseDamage = baseDamage;
        }

        @Override
        public String id() {
            return "test.capture";
        }

        @Override
        public String description() {
            return "capture";
        }

        @Override
        public void execute(GuScriptContext context) {
            captured = context.applyDamageModifiers(baseDamage);
        }

        double capturedDamage() {
            return captured;
        }
    }

    private static final class RecordModifiersAction implements net.tigereye.chestcavity.guscript.ast.Action {
        private double multiplier;
        private double flat;

        @Override
        public String id() {
            return "test.record_modifiers";
        }

        @Override
        public String description() {
            return "record";
        }

        @Override
        public void execute(GuScriptContext context) {
            multiplier = context.damageMultiplier();
            flat = context.flatDamageBonus();
        }

        double multiplier() {
            return multiplier;
        }

        double flat() {
            return flat;
        }
    }
}

