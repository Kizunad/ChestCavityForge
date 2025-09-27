package net.tigereye.chestcavity.guscript.data;

import net.tigereye.chestcavity.guscript.ast.GuNode;

import java.util.List;

/**
 * Stores cached compilation output for a GuScript page binding.
 */
public record GuScriptProgramCache(List<GuNode> roots, int inventorySignature, long compiledGameTime) {
    public GuScriptProgramCache {
        roots = List.copyOf(roots);
    }
}
