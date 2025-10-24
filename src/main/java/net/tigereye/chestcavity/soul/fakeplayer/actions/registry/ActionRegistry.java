package net.tigereye.chestcavity.soul.fakeplayer.actions.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.Action;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionId;

public final class ActionRegistry {
  private static final Map<ResourceLocation, Action> ACTIONS = new ConcurrentHashMap<>();
  private static final java.util.List<ActionFactory> FACTORIES =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  private ActionRegistry() {}

  public static void register(Action action) {
    ACTIONS.put(action.id().id(), action);
  }

  public static void registerFactory(ActionFactory factory) {
    if (factory != null) FACTORIES.add(factory);
  }

  public static Action find(ResourceLocation id) {
    return ACTIONS.get(id);
  }

  public static Action find(ActionId id) {
    return find(id.id());
  }

  public static Action resolveOrCreate(ResourceLocation id) {
    Action a = ACTIONS.get(id);
    if (a != null) return a;
    for (ActionFactory f : FACTORIES) {
      if (f.supports(id)) {
        a = f.create(id);
        if (a != null) {
          ACTIONS.put(id, a);
          return a;
        }
      }
    }
    return null;
  }

  public static Collection<Action> all() {
    return Collections.unmodifiableCollection(ACTIONS.values());
  }
}
