package net.tigereye.chestcavity.soul.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Context for flee behaviour. */
public record FleeContext(ServerLevel level, SoulPlayer self, LivingEntity threat, Vec3 anchor) {
    public static FleeContext of(SoulPlayer self, LivingEntity threat, Vec3 anchor) {
        return new FleeContext(self.serverLevel(), self, threat, anchor);
    }
}

