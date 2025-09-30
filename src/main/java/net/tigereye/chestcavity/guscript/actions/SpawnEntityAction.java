package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.util.EntitySpawnUtil;

/**
 * Minimal generic action to spawn an entity by id near the performer.
 */
public final class SpawnEntityAction implements Action {

    public static final String ID = "spawn.entity";

    private final ResourceLocation entityId;
    private final Vec3 offset;
    private final boolean noAi;

    public SpawnEntityAction(ResourceLocation entityId, Vec3 offset, boolean noAi) {
        this.entityId = entityId;
        this.offset = offset == null ? Vec3.ZERO : offset;
        this.noAi = noAi;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Spawn entity " + entityId;
    }

    @Override
    public void execute(GuScriptContext context) {
        Player performer = context == null ? null : context.performer();
        if (performer == null || !(performer.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = performer.position().add(offset);
        float yaw = performer.getYRot();
        float pitch = performer.getXRot();
        EntitySpawnUtil.spawn(level, entityId, pos, yaw, pitch, noAi);
    }
}

