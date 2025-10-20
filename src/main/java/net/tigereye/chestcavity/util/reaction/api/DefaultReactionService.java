package net.tigereye.chestcavity.util.reaction.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.util.reaction.ReactionRegistry;

/**
 * 默认 ReactionService：委托到 {@link ReactionRegistry} 现有实现。
 */
public final class DefaultReactionService implements ReactionService {
    @Override
    public boolean preApplyDoT(MinecraftServer server, ResourceLocation dotTypeId, LivingEntity attacker, LivingEntity target) {
        return ReactionRegistry.preApplyDoT(server, dotTypeId, attacker, target);
    }
}

