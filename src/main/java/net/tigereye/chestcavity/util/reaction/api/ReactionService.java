package net.tigereye.chestcavity.util.reaction.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

/** 反应系统的最小 API，供 DoT 等子系统调用以在伤害前进行反应判定/拦截。 */
public interface ReactionService {

  /**
   * 在 DoT 伤害应用前进行反应判定。
   *
   * @return true 继续结算；false 取消本次 DoT 伤害（反应已在内部处理）
   */
  boolean preApplyDoT(
      MinecraftServer server,
      ResourceLocation dotTypeId,
      LivingEntity attacker,
      LivingEntity target);
}
