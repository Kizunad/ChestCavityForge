package net.tigereye.chestcavity.compat.guzhenren.linkage.effect;

/**
 * Functional contract for registering Guzhenren organ linkage behaviours.
 * Implementations receive a {@link LinkageEffectContext} exposing helper methods
 * for adding listeners, accessing the owning chest cavity and inspecting organ state.
 */
@FunctionalInterface
public interface LinkageEffect {

    /** Applies the effect to the provided context. */
    void apply(LinkageEffectContext context);
}
