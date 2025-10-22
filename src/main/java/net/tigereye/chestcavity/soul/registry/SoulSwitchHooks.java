package net.tigereye.chestcavity.soul.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Simple in-memory registry for switch hooks. */
public final class SoulSwitchHooks {

  private static final List<SoulSwitchHook> HOOKS = new ArrayList<>();

  private SoulSwitchHooks() {}

  public static void register(SoulSwitchHook hook) {
    if (hook != null) HOOKS.add(hook);
  }

  public static void preSwitch(ServerPlayer executor, UUID currentId, UUID targetId) {
    for (SoulSwitchHook hook : HOOKS) hook.preSwitch(executor, currentId, targetId);
  }

  public static void postSwitch(
      ServerPlayer executor, UUID previousId, UUID targetId, boolean success) {
    for (SoulSwitchHook hook : HOOKS) hook.postSwitch(executor, previousId, targetId, success);
  }
}
