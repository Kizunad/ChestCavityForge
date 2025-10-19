package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Lightweight Mob used only to host PathNavigation/MoveControl logic in-memory.
 *
 * IMPORTANT: Instances are never added to the Level. They only reference the Level for
 * block/entity collision queries during navigation.
 */
final class DummyMob extends Mob {

    // We reuse a vanilla Mob entity type to keep attribute maps available without registry work.
    // This entity is never spawned, so its visual/behavioral identity is irrelevant.
    DummyMob(Level level) {
        super(EntityType.PIG, level); // safe reuse; not spawned into world
        this.noPhysics = false;
        this.setNoAi(false);
        this.setInvisible(true);
        this.setSilent(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // no goals
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        var nav = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
        // Encourage small step-ups by enabling passing through doors and floating; jumping is handled by path logic
        nav.setCanPassDoors(true);
        nav.setCanOpenDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean isNoAi() {
        return false;
    }

    // Attribute supplier must match a typical land mob so navigation math is valid.
    public static AttributeSupplier.Builder createAttributes() {
        return Pig.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 1.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 20.0D);
    }
}
