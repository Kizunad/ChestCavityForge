package net.tigereye.chestcavity.engine.reaction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.util.reaction.ReactionRegistry;
import net.tigereye.chestcavity.util.reaction.api.ReactionAPI;

/** Reaction 核心引擎（门面）。 - 统一由引擎层对外暴露 API；内部委托到现有 ReactionRegistry/ReactionAPI 实现。 */
public final class ReactionEngine {

  private ReactionEngine() {}

  public static void bootstrap() {
    ReactionRegistry.bootstrap();
  }

  public static boolean preApplyDoT(
      MinecraftServer server,
      ResourceLocation dotTypeId,
      LivingEntity attacker,
      LivingEntity target) {
    return ReactionAPI.get().preApplyDoT(server, dotTypeId, attacker, target);
  }
}
