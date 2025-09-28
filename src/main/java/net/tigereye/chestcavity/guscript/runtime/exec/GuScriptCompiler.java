package net.tigereye.chestcavity.guscript.runtime.exec;

import com.google.common.collect.HashMultiset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.data.GuScriptProgramCache;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.guscript.registry.GuScriptRegistry;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compiles the contents of a GuScript page into executable AST roots, caching the results when possible.
 */
public final class GuScriptCompiler {

    private static final GuScriptReducer REDUCER = new GuScriptReducer();

    private GuScriptCompiler() {
    }

    public static GuScriptProgramCache compileIfNeeded(GuScriptAttachment attachment, long gameTime) {
        return compilePageIfNeeded(attachment, attachment.activePage(), attachment.getCurrentPageIndex(), gameTime);
    }

    public static GuScriptProgramCache compilePageIfNeeded(
            GuScriptAttachment attachment,
            GuScriptPageState page,
            int pageIndex,
            long gameTime
    ) {
        if (attachment == null || page == null) {
            return new GuScriptProgramCache(List.of(), 0, gameTime);
        }
        int signature = computeSignature(page);
        GuScriptProgramCache cached = page.compiledProgram();
        if (cached != null && cached.inventorySignature() == signature && !page.consumeDirtyFlag()) {
            return cached;
        }

        List<LeafGuNode> leaves = new ArrayList<>();
        int bindingSlot = page.items().size() - 1;
        for (int i = 0; i < bindingSlot; i++) {
            ItemStack stack = page.items().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = stack.getItem().builtInRegistryHolder().key().location();
            GuScriptRegistry.leaf(itemId).ifPresentOrElse(def -> {
                leaves.add(toScaledLeaf(def, stack.getCount()));
            }, () -> ChestCavity.LOGGER.warn("[GuScript] No leaf definition for item {}", itemId));
        }

        List<GuNode> roots = new ArrayList<>();
        if (!leaves.isEmpty()) {
            List<ReactionRule> rules = GuScriptRegistry.reactionRules();
            GuScriptReducer.ReductionResult result = REDUCER.reduce(new ArrayList<>(leaves), rules);
            roots.addAll(result.roots());
            ChestCavity.LOGGER.info(
                    "[GuScript] Compiled page {} (binding={}, listener={}) -> {} roots: {}",
                    pageIndex,
                    page.bindingTarget(),
                    page.listenerType(),
                    roots.size(),
                    roots.stream()
                            .map(node -> node.kind() + ":" + node.name())
                            .toList()
            );
        } else {
            ChestCavity.LOGGER.info(
                    "[GuScript] Compiled page {} (binding={}, listener={}) -> empty root set",
                    pageIndex,
                    page.bindingTarget(),
                    page.listenerType()
            );
        }

        GuScriptProgramCache program = new GuScriptProgramCache(roots, signature, gameTime);
        page.setCompiledProgram(program);
        page.setInventorySignature(signature);
        page.consumeDirtyFlag();
        return program;
    }

    static LeafGuNode toScaledLeaf(GuScriptRegistry.LeafDefinition definition, int count) {
        int scaledCount = Math.max(1, count);
        HashMultiset<String> scaledTags = HashMultiset.create();
        definition.tags().forEachEntry((tag, tagCount) -> scaledTags.add(tag, tagCount * scaledCount));
        return new LeafGuNode(definition.name(), scaledTags, definition.actions());
    }

    private static int computeSignature(GuScriptPageState page) {
        int hash = 1;
        int bindingSlot = page.items().size() - 1;
        for (int i = 0; i < bindingSlot; i++) {
            ItemStack stack = page.items().get(i);
            if (stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }
            ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
            hash = 31 * hash + Objects.hash(id, stack.getCount(), stack.getDamageValue());
        }
        hash = 31 * hash + page.bindingTarget().ordinal();
        hash = 31 * hash + page.listenerType().ordinal();
        hash = 31 * hash + page.flowId().map(Object::hashCode).orElse(0);
        Map<String, String> params = page.flowParams();
        if (!params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                hash = 31 * hash + Objects.hash(entry.getKey(), entry.getValue());
            }
        }
        return hash;
    }
}
