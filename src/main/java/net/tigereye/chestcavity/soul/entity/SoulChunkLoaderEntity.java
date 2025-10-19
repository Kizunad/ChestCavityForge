package net.tigereye.chestcavity.soul.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * 极简区块加载器实体：保持指定半径内的区块常驻加载。
 *
 * <p>该实体仅在服务端存续，默认为不可见、不可交互。</p>
 */
public class SoulChunkLoaderEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_RADIUS =
            SynchedEntityData.defineId(SoulChunkLoaderEntity.class, EntityDataSerializers.INT);
    private static final TicketType<UUID> CHUNK_TICKET =
            TicketType.create("chestcavity:soul_chunk_loader", UUID::compareTo);

    @Nullable
    private ChunkPos ticketPos;
    private int ticketRadius;

    public SoulChunkLoaderEntity(EntityType<? extends SoulChunkLoaderEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 2);
    }

    public int getTicketRadius() {
        return Math.max(1, this.entityData.get(DATA_RADIUS));
    }

    public void setTicketRadius(int radius) {
        radius = Math.max(1, radius);
        this.entityData.set(DATA_RADIUS, radius);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            updateChunkLoading(server);
        }
    }

    private void updateChunkLoading(ServerLevel level) {
        ChunkPos chunkPos = new ChunkPos(this.blockPosition());
        int radius = getTicketRadius();
        if (!chunkPos.equals(ticketPos) || radius != ticketRadius) {
            releaseTicket(level);
            level.getChunkSource().addRegionTicket(CHUNK_TICKET, chunkPos, radius, this.getUUID());
            ticketPos = chunkPos;
            ticketRadius = radius;
        }
    }

    private void releaseTicket(@Nullable ServerLevel level) {
        if (level != null && ticketPos != null && ticketRadius > 0) {
            level.getChunkSource().removeRegionTicket(CHUNK_TICKET, ticketPos, ticketRadius, this.getUUID());
        }
        ticketPos = null;
        ticketRadius = 0;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("radius")) {
            setTicketRadius(tag.getInt("radius"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("radius", getTicketRadius());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            releaseTicket(server);
        }
        super.remove(reason);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // no-op
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDiff, boolean onGround, BlockState state, BlockPos pos) {
        // ignore
    }
}
