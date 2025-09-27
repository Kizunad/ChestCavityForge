package net.tigereye.chestcavity.guscript.runtime.exec;

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
import java.util.Objects;

/**
 * Compiles the contents of a GuScript page into executable AST roots, caching the results when possible.
 */
public final class GuScriptCompiler {

    private static final GuScriptReducer REDUCER = new GuScriptReducer();

    private GuScriptCompiler() {
    }

    public static GuScriptProgramCache compileIfNeeded(GuScriptAttachment attachment, long gameTime) {
        GuScriptPageState page = attachment.activePage();
        int signature = computeSignature(page);
        GuScriptProgramCache cached = page.compiledProgram();
        if (cached != null && cached.inventorySignature() == signature && !page.consumeDirtyFlag()) {
            return cached;
        }

        List<LeafGuNode> leaves = new ArrayList<>();
        for (int i = 0; i < GuScriptAttachment.ITEM_SLOT_COUNT; i++) {
            ItemStack stack = page.items().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = stack.getItem().builtInRegistryHolder().key().location();
            GuScriptRegistry.leaf(itemId).ifPresentOrElse(def -> {
                leaves.add(def.toNode());
            }, () -> ChestCavity.LOGGER.debug("[GuScript] No leaf definition for item {}", itemId));
        }

        List<GuNode> roots = new ArrayList<>();
        if (!leaves.isEmpty()) {
            List<ReactionRule> rules = GuScriptRegistry.reactionRules();
            GuScriptReducer.ReductionResult result = REDUCER.reduce(new ArrayList<>(leaves), rules);
            roots.addAll(result.roots());
        }

        GuScriptProgramCache program = new GuScriptProgramCache(roots, signature, gameTime);
        page.setCompiledProgram(program);
        page.setInventorySignature(signature);
        page.consumeDirtyFlag();
        return program;
    }

    private static int computeSignature(GuScriptPageState page) {
        int hash = 1;
        for (int i = 0; i < GuScriptAttachment.TOTAL_SLOTS; i++) {
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
        return hash;
    }
}
