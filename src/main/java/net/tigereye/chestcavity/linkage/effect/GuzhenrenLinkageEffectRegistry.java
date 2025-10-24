package net.tigereye.chestcavity.linkage.effect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Central registry that maps Guzhenren organ item combinations to linkage effects. Registration
 * is declarative and keeps listener wiring consistent across organs.
 */
public final class GuzhenrenLinkageEffectRegistry {

  private static final Map<ResourceLocation, List<LinkageEffectDefinition>> DEFINITIONS =
      new HashMap<>();

  private GuzhenrenLinkageEffectRegistry() {}

  /** Registers an effect that is triggered when the primary organ is present. */
  public static LinkageEffectDefinition registerEffect(
      ResourceLocation primaryOrgan,
      Collection<ResourceLocation> requiredOrgans,
      LinkageEffect effect) {
    Objects.requireNonNull(primaryOrgan, "primaryOrgan");
    Objects.requireNonNull(requiredOrgans, "requiredOrgans");
    Objects.requireNonNull(effect, "effect");
    if (requiredOrgans.isEmpty()) {
      throw new IllegalArgumentException("requiredOrgans must not be empty");
    }
    Map<ResourceLocation, Integer> requirements = normalise(requiredOrgans, primaryOrgan);
    LinkageEffectDefinition definition =
        new LinkageEffectDefinition(primaryOrgan, requirements, effect);
    synchronized (DEFINITIONS) {
      DEFINITIONS.computeIfAbsent(primaryOrgan, unused -> new ArrayList<>()).add(definition);
    }
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[Guzhenren] Registered linkage effect for {} with requirements {}",
          primaryOrgan,
          requirements);
    }
    return definition;
  }

  /** Registers an effect where the first organ in {@code organIds} acts as the primary trigger. */
  public static LinkageEffectDefinition registerEffect(
      List<ResourceLocation> organIds, LinkageEffect effect) {
    if (organIds == null || organIds.isEmpty()) {
      throw new IllegalArgumentException("organIds must not be empty");
    }
    ResourceLocation primary = organIds.get(0);
    return registerEffect(primary, organIds, effect);
  }

  /** Convenience helper for single-organ effects. */
  public static LinkageEffectDefinition registerSingle(
      ResourceLocation organId, LinkageEffect effect) {
    return registerEffect(organId, Collections.singletonList(organId), effect);
  }

  /** Applies all effects whose primary organ matches the provided stack. */
  public static boolean applyEffects(
      ChestCavityInstance cc,
      ItemStack stack,
      List<OrganRemovalContext> staleRemovalContexts,
      Map<ResourceLocation, Integer> cachedCounts,
      Map<ResourceLocation, List<ItemStack>> cachedStacks) {
    if (cc == null || stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    if (itemId == null) {
      if (ChestCavity.LOGGER.isTraceEnabled()) {
        ChestCavity.LOGGER.trace(
            "[Guzhenren] Unable to resolve item id for stack {} while applying effects", stack);
      }
      return false;
    }
    List<LinkageEffectDefinition> definitions;
    synchronized (DEFINITIONS) {
      definitions = DEFINITIONS.get(itemId);
    }
    if (definitions == null || definitions.isEmpty()) {
      if (ChestCavity.LOGGER.isTraceEnabled()) {
        ChestCavity.LOGGER.trace("[Guzhenren] No linkage effects registered for {}", itemId);
      }
      return false;
    }
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[Guzhenren] Applying {} linkage definitions for item {} on cavity {}",
          definitions.size(),
          itemId,
          cc == null || cc.owner == null ? "<unbound>" : cc.owner.getScoreboardName());
    }
    boolean applied = false;
    for (LinkageEffectDefinition definition : definitions) {
      if (ChestCavity.LOGGER.isTraceEnabled()) {
        ChestCavity.LOGGER.trace(
            "[Guzhenren] Evaluating linkage definition {} for {}", definition, itemId);
      }
      Optional<LinkageEffectContext> contextOpt =
          definition.createContext(cc, stack, staleRemovalContexts, cachedCounts, cachedStacks);
      if (contextOpt.isEmpty()) {
        if (ChestCavity.LOGGER.isTraceEnabled()) {
          ChestCavity.LOGGER.trace(
              "[Guzhenren] Requirements not met for linkage definition {} on {}",
              definition,
              itemId);
        }
        continue;
      }
      if (ChestCavity.LOGGER.isDebugEnabled()) {
        ChestCavity.LOGGER.debug(
            "[Guzhenren] Executing linkage effect {} for {}", definition.effect(), itemId);
      }
      definition.effect().apply(contextOpt.get());
      applied = true;
    }
    if (!applied && ChestCavity.LOGGER.isTraceEnabled()) {
      ChestCavity.LOGGER.trace("[Guzhenren] No linkage effects executed for {}", itemId);
    }
    return applied;
  }

  private static Map<ResourceLocation, Integer> normalise(
      Collection<ResourceLocation> organs, ResourceLocation primary) {
    Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
    for (ResourceLocation id : organs) {
      if (id == null) {
        continue;
      }
      counts.merge(id, 1, Integer::sum);
    }
    counts.putIfAbsent(primary, 1);
    return counts;
  }

  /** Encapsulates a linkage effect definition with its requirements. */
  public record LinkageEffectDefinition(
      ResourceLocation primaryOrgan,
      Map<ResourceLocation, Integer> requirements,
      LinkageEffect effect) {

    public LinkageEffectDefinition {
      Objects.requireNonNull(primaryOrgan, "primaryOrgan");
      Objects.requireNonNull(requirements, "requirements");
      Objects.requireNonNull(effect, "effect");
      requirements = Collections.unmodifiableMap(new LinkedHashMap<>(requirements));
    }

    private Optional<LinkageEffectContext> createContext(
        ChestCavityInstance cc,
        ItemStack sourceOrgan,
        List<OrganRemovalContext> staleRemovalContexts,
        Map<ResourceLocation, Integer> cachedCounts,
        Map<ResourceLocation, List<ItemStack>> cachedStacks) {
      if (cc == null || sourceOrgan == null || sourceOrgan.isEmpty()) {
        return Optional.empty();
      }
      Map<ResourceLocation, Integer> presentCounts = new HashMap<>();
      List<ItemStack> matchingStacks = new ArrayList<>();
      if (cachedCounts != null && cachedStacks != null) {
        for (ResourceLocation requirementId : requirements.keySet()) {
          int count = cachedCounts.getOrDefault(requirementId, 0);
          if (count > 0) {
            presentCounts.put(requirementId, count);
            List<ItemStack> stacks = cachedStacks.get(requirementId);
            if (stacks != null && !stacks.isEmpty()) {
              matchingStacks.addAll(stacks);
            }
          }
        }
      } else {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
          ItemStack slotStack = cc.inventory.getItem(i);
          if (slotStack == null || slotStack.isEmpty()) {
            continue;
          }
          ResourceLocation id = BuiltInRegistries.ITEM.getKey(slotStack.getItem());
          if (id == null || !requirements.containsKey(id)) {
            continue;
          }
          matchingStacks.add(slotStack);
          presentCounts.merge(id, Math.max(1, slotStack.getCount()), Integer::sum);
        }
      }
      for (Map.Entry<ResourceLocation, Integer> requirement : requirements.entrySet()) {
        int have = presentCounts.getOrDefault(requirement.getKey(), 0);
        if (have < requirement.getValue()) {
          if (ChestCavity.LOGGER.isTraceEnabled()) {
            ChestCavity.LOGGER.trace(
                "[Guzhenren] Missing requirement {}x{} for {} (found {})",
                requirement.getValue(),
                requirement.getKey(),
                primaryOrgan,
                have);
          }
          return Optional.empty();
        }
      }
      if (ChestCavity.LOGGER.isDebugEnabled()) {
        ChestCavity.LOGGER.debug(
            "[Guzhenren] Linkage definition for {} matched stacks {} (counts {})",
            primaryOrgan,
            matchingStacks,
            presentCounts);
      }
      return Optional.of(
          new DefaultLinkageEffectContext(
              cc, sourceOrgan, requirements, matchingStacks, presentCounts, staleRemovalContexts));
    }

    @Override
    public String toString() {
      return "LinkageEffectDefinition{"
          + "primary="
          + primaryOrgan
          + ", requirements="
          + requirements
          + '}';
    }
  }
}
