package net.tigereye.chestcavity.soul.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Lightweight context passed to attack handlers. */
public record AttackContext(ServerLevel level, SoulPlayer self, LivingEntity target, double distance) {
    public static AttackContext of(SoulPlayer self, LivingEntity target) {
        double d = self.distanceTo(target);
        return new AttackContext(self.serverLevel(), self, target, d);
    }

    public ServerPlayer ownerOrNull() {
        var id = self.getOwnerId().orElse(null);
        return id == null ? null : level.getServer().getPlayerList().getPlayer(id);
    }
}

