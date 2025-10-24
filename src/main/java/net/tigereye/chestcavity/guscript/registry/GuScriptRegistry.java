package net.tigereye.chestcavity.guscript.registry;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;

/** Central registry storing GuScript reaction rules and leaf definitions loaded from data packs. */
public final class GuScriptRegistry {

  private static final Map<ResourceLocation, LeafDefinition> LEAVES = new HashMap<>();
  private static List<ReactionRule> reactionRules = List.of();

  private GuScriptRegistry() {}

  public static synchronized void updateLeaves(Map<ResourceLocation, LeafDefinition> entries) {
    LEAVES.clear();
    LEAVES.putAll(entries);
    ChestCavity.LOGGER.info("[GuScript] Loaded {} leaf definitions", LEAVES.size());
  }

  public static synchronized void updateReactionRules(List<ReactionRule> rules) {
    reactionRules = List.copyOf(rules);
    ChestCavity.LOGGER.info("[GuScript] Loaded {} reaction rules", reactionRules.size());
  }

  public static synchronized Optional<LeafDefinition> leaf(ResourceLocation id) {
    return Optional.ofNullable(LEAVES.get(id));
  }

  public static synchronized List<ReactionRule> reactionRules() {
    return reactionRules;
  }

  public static synchronized Set<ResourceLocation> leafIds() {
    return Collections.unmodifiableSet(LEAVES.keySet());
  }

  public record LeafDefinition(String name, Multiset<String> tags, List<Action> actions) {
    public LeafDefinition {
      tags = tags == null ? ImmutableMultiset.of() : ImmutableMultiset.copyOf(tags);
      actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public LeafGuNode toNode() {
      return new LeafGuNode(name, tags, actions);
    }
  }
}
