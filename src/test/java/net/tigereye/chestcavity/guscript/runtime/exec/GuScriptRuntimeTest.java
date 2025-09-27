package net.tigereye.chestcavity.guscript.runtime.exec;

import com.google.common.collect.ImmutableMultiset;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.actions.AddDamageMultiplierAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
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
}

