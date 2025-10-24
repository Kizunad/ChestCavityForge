package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 简化的冲锋体（服务端权威），生成后沿指定方向高速移动，撞到非同盟单位造成伤害与击退，存活少量tick后消散。 说明：实现为自定义 Entity（非
 * Mob），以避免复杂AI与渲染需求；符合“直接生成并冲刺”的期望。
 */
public class MadBullEntity extends Entity {

  private static final int DEFAULT_LIFESPAN =
      BehaviorConfigAccess.getInt(MadBullEntity.class, "LIFESPAN_TICKS", 40);
  private static final double DEFAULT_SPEED =
      BehaviorConfigAccess.getFloat(MadBullEntity.class, "SPEED_PER_TICK", 1.4f);
  private static final float DEFAULT_DAMAGE =
      BehaviorConfigAccess.getFloat(MadBullEntity.class, "DAMAGE", 8.0f);
  private static final double HIT_RADIUS =
      BehaviorConfigAccess.getFloat(MadBullEntity.class, "HIT_RADIUS", 0.8f);

  private UUID ownerId;
  private LivingEntity cachedOwner;
  private Vec3 direction = Vec3.ZERO;
  private int age;
  private int lifespan = DEFAULT_LIFESPAN;
  private float damage = DEFAULT_DAMAGE;
  private final Set<UUID> hitOnce = new HashSet<>();

  public MadBullEntity(EntityType<? extends MadBullEntity> type, Level level) {
    super(type, level);
    this.noPhysics = false;
  }

  public void setOwner(LivingEntity owner) {
    this.ownerId = owner == null ? null : owner.getUUID();
    this.cachedOwner = owner;
  }

  public LivingEntity getOwner() {
    if (cachedOwner != null && !cachedOwner.isRemoved()) {
      return cachedOwner;
    }
    if (ownerId != null && this.level() instanceof ServerLevel server) {
      Entity e = server.getEntity(ownerId);
      if (e instanceof LivingEntity living) {
        cachedOwner = living;
        return living;
      }
    }
    return null;
  }

  public void setChargeDirection(Vec3 dir) {
    if (dir == null || dir.lengthSqr() < 1.0E-6) {
      this.direction = new Vec3(1, 0, 0);
    } else {
      this.direction = dir.normalize();
    }
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {}

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    this.age = tag.getInt("Age");
    this.lifespan = Math.max(1, tag.getInt("Lifespan"));
    this.damage = tag.getFloat("Damage");
    if (tag.hasUUID("Owner")) {
      this.ownerId = tag.getUUID("Owner");
    }
    double dx = tag.getDouble("dx");
    double dy = tag.getDouble("dy");
    double dz = tag.getDouble("dz");
    this.direction = new Vec3(dx, dy, dz);
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.putInt("Age", age);
    tag.putInt("Lifespan", lifespan);
    tag.putFloat("Damage", damage);
    if (ownerId != null) tag.putUUID("Owner", ownerId);
    tag.putDouble("dx", direction.x);
    tag.putDouble("dy", direction.y);
    tag.putDouble("dz", direction.z);
  }

  @Override
  public void tick() {
    super.tick();
    if (this.level().isClientSide) {
      return;
    }
    Vec3 step = direction.scale(DEFAULT_SPEED);
    Vec3 start = this.position();
    Vec3 end = start.add(step);

    // 伤害判定
    if (this.level() instanceof ServerLevel server) {
      AABB sweep = new AABB(start, end).inflate(HIT_RADIUS);
      for (LivingEntity target :
          server.getEntitiesOfClass(LivingEntity.class, sweep, this::canHit)) {
        if (hitOnce.add(target.getUUID())) {
          applyHit(target);
        }
      }
    }

    this.move(MoverType.SELF, step);
    age++;
    if (age >= lifespan) {
      discard();
    }
  }

  private boolean canHit(LivingEntity target) {
    if (!target.isAlive()) return false;
    LivingEntity owner = getOwner();
    if (owner != null) {
      if (target == owner) return false;
      if (target.isAlliedTo(owner)) return false;
    }
    return true;
  }

  private void applyHit(LivingEntity target) {
    LivingEntity owner = getOwner();
    DamageSource src;
    if (owner instanceof net.minecraft.world.entity.player.Player player) {
      src = this.damageSources().playerAttack(player);
    } else if (owner != null) {
      src = this.damageSources().mobAttack(owner);
    } else {
      src = this.damageSources().generic();
    }
    float amount = Math.max(0.0f, damage);
    if (amount <= 0.0f) return;
    if (target.hurt(src, amount)) {
      Vec3 push = direction.lengthSqr() > 1.0E-6 ? direction : this.getForward();
      if (push.lengthSqr() > 1.0E-6) {
        push = push.normalize();
        target.push(push.x * 0.9D, 0.15D + Math.abs(push.y) * 0.25D, push.z * 0.9D);
        target.hurtMarked = true;
      }
    }
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
    return new ClientboundAddEntityPacket(this, serverEntity);
  }
}
