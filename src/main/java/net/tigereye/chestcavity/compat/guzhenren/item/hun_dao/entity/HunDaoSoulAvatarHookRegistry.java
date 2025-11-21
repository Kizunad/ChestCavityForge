package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 简易 Hook 注册中心，供魂道分身核心事件复用。
 */
public final class HunDaoSoulAvatarHookRegistry {

  private static final Map<ResourceLocation, HunDaoSoulAvatarHook> HOOKS = new LinkedHashMap<>();

  private HunDaoSoulAvatarHookRegistry() {}

  public static void register(ResourceLocation id, HunDaoSoulAvatarHook hook) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(hook, "hook");
    HOOKS.put(id, hook);
  }

  public static Collection<HunDaoSoulAvatarHook> getHooks() {
    return Collections.unmodifiableCollection(HOOKS.values());
  }

  private static void broadcast(Consumer<HunDaoSoulAvatarHook> invoker) {
    if (HOOKS.isEmpty()) {
      return;
    }
    for (HunDaoSoulAvatarHook hook : HOOKS.values()) {
      try {
        invoker.accept(hook);
      } catch (Exception exception) {
        ChestCavity.LOGGER.warn(
            "[hun_dao] Soul avatar hook {} failed: {}",
            hook.getClass().getSimpleName(),
            exception.getMessage());
      }
    }
  }

  public static void dispatchSpawn(HunDaoSoulAvatarEntity avatar) {
    broadcast(hook -> hook.onSpawn(avatar));
  }

  public static void dispatchServerTick(HunDaoSoulAvatarEntity avatar) {
    broadcast(hook -> hook.onServerTick(avatar));
  }

  public static void dispatchKill(HunDaoSoulAvatarEntity avatar, LivingEntity victim) {
    broadcast(hook -> hook.onKillEntity(avatar, victim));
  }

  public static void dispatchDeath(HunDaoSoulAvatarEntity avatar, DamageSource source) {
    broadcast(hook -> hook.onDeath(avatar, source));
  }

  public static void dispatchRemoved(HunDaoSoulAvatarEntity avatar, Entity.RemovalReason reason) {
    broadcast(hook -> hook.onRemoved(avatar, reason));
  }
}
