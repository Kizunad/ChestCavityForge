package net.tigereye.chestcavity.guscript.ast;

/**
 * Describes an atomic action that a node can execute when the AST is interpreted.
 * Actions are lightweight identifiers with an optional description for UI/debug use.
 */
public record Action(String id, String description) {
    public Action {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Action id must be non-empty");
        }
        description = description == null ? "" : description;
    }
}
