package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;

/**
 * 魂道通用分身实体：复制玩家皮肤/资源，暴露 HookRegistry。
 */
public class HunDaoSoulAvatarEntity extends PathfinderMob implements OwnableEntity {

  private static final EntityDataAccessor<Optional<UUID>> OWNER =
      SynchedEntityData.defineId(HunDaoSoulAvatarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
  private static final EntityDataAccessor<String> SKIN_TEXTURE =
      SynchedEntityData.defineId(HunDaoSoulAvatarEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> SKIN_MODEL =
      SynchedEntityData.defineId(HunDaoSoulAvatarEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Integer> SKIN_COLOR =
      SynchedEntityData.defineId(HunDaoSoulAvatarEntity.class, EntityDataSerializers.INT);

  private static final ResourceLocation DEFAULT_TEXTURE =
      ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
  private static final ResourceLocation DEFAULT_TEMPLATE_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/default_avatar");
  private static final int DEFAULT_TINT = 0x8090A0FF;

  private static final double HUNPO_PER_SCALE_STEP =
      HunDaoRuntimeTuning.SoulAvatarScaling.HUNPO_PER_SCALE_STEP;

  private HunDaoSoulAvatarResourceState resourceState;
  private boolean spawnHookDispatched = false;
  private ResourceLocation templateId = DEFAULT_TEMPLATE_ID;
  private int lifetimeTicks = -1;

  public HunDaoSoulAvatarEntity(EntityType<? extends PathfinderMob> type, Level level) {
    super(type, level);
    this.setNoAi(false);
    this.setPersistenceRequired();
    this.noCulling = true;
  }

  public static AttributeSupplier.Builder createMobAttributes() {
    return PathfinderMob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 40.0D)
        .add(Attributes.ATTACK_DAMAGE, 6.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.3D);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(OWNER, Optional.empty());
    builder.define(SKIN_TEXTURE, DEFAULT_TEXTURE.toString());
    builder.define(SKIN_MODEL, PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
    builder.define(SKIN_COLOR, DEFAULT_TINT);
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
    this.goalSelector.addGoal(8, new FollowOwnerGoal(this, 10.0f, 2.5f));
    this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
    this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0f));
    this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
  }

  @Override
  public void tick() {
    super.tick();
    if (!level().isClientSide) {
      HunDaoSoulAvatarHookRegistry.dispatchServerTick(this);
    }
  }

  @Override
  public void onAddedToLevel() {
    super.onAddedToLevel();
    if (!level().isClientSide && !this.spawnHookDispatched) {
      HunDaoSoulAvatarHookRegistry.dispatchSpawn(this);
      this.spawnHookDispatched = true;
    }
  }

  @Override
  public boolean killedEntity(ServerLevel level, LivingEntity victim) {
    boolean result = super.killedEntity(level, victim);
    if (!level.isClientSide) {
      HunDaoSoulAvatarHookRegistry.dispatchKill(this, victim);
    }
    return result;
  }

  @Override
  public void die(DamageSource source) {
    if (!level().isClientSide) {
      HunDaoSoulAvatarHookRegistry.dispatchDeath(this, source);
    }
    super.die(source);
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (level().isClientSide || amount <= 0.0F) {
      return super.hurt(source, amount);
    }
    double costPerDamage = getHunpoCostPerDamage();
    if (!(costPerDamage > 0.0D)) {
      return super.hurt(source, amount);
    }
    double hunpoCost = Math.max(0.0D, amount) * costPerDamage;
    double remainingDamage = amount;
    Optional<ResourceHandle> handleOpt = ResourceOps.openHandle(this);
    if (handleOpt.isPresent()) {
      ResourceHandle handle = handleOpt.get();
      double available = handle.getHunpo().orElse(0.0D);
      double consumed = Math.min(available, hunpoCost);
      if (consumed > 0.0D) {
        handle.adjustHunpo(-consumed, true);
      }
      double remainingCost = Math.max(0.0D, hunpoCost - consumed);
      remainingDamage = remainingCost / costPerDamage;
      double current = handle.getHunpo().orElse(0.0D);
      double maxHunpo = handle.getMaxHunpo().orElse(0.0D);
      getResourceState().setHunpoSnapshot(current, maxHunpo);
      refreshDimensions();
    }
    if (remainingDamage <= 0.0D) {
      return true;
    }
    double mitigated = applyScarMitigation(remainingDamage);
    if (mitigated <= 0.0D) {
      return true;
    }
    return super.hurt(source, (float) mitigated);
  }

  protected double getHunpoCostPerDamage() {
    return 1.0D;
  }

  private double applyScarMitigation(double damage) {
    return HunDaoSoulAvatarHookRegistry.dispatchModifyDamage(this, damage);
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    if (!level().isClientSide) {
      HunDaoSoulAvatarHookRegistry.dispatchRemoved(this, reason);
    }
    super.remove(reason);
  }

  public void initialiseFromOwner(ServerPlayer owner) {
    if (owner == null) {
      return;
    }
    setOwnerUUID(owner.getUUID());
    applyOwnerSkin(owner);
    copyResourcesFromOwner(owner);
  }

  public void applyOwnerSkin(Player owner) {
    PlayerSkinUtil.SkinSnapshot snapshot = PlayerSkinUtil.capture(owner);
    PlayerSkinUtil.SkinSnapshot tint =
        PlayerSkinUtil.withTint(snapshot, 0.6f, 0.6f, 1.0f, 0.5f);
    setSkin(tint);
  }

  public void copyResourcesFromOwner(Player owner) {
    ResourceHandle ownerHandle = ResourceOps.openHandle(owner).orElse(null);
    if (ownerHandle == null) {
      return;
    }
    HunDaoSoulAvatarResourceState state = ensureResourceState();
    state.copyFrom(ownerHandle);
    ResourceOps.openHandle(this).ifPresent(state::applyTo);
    this.refreshDimensions();
  }

  @Override
  public void refreshDimensions() {
    super.refreshDimensions();
    syncHunpoToHealth();
  }

  protected void syncHunpoToHealth() {
    HunDaoSoulAvatarHookRegistry.dispatchSyncHealth(this);
  }

  public void refreshSnapshotFromHandle() {
    ResourceOps.openHandle(this)
        .ifPresent(
            handle -> {
              ensureResourceState().copyFrom(handle);
              this.refreshDimensions();
            });
  }

  public HunDaoSoulAvatarResourceState getResourceState() {
    return ensureResourceState();
  }

  public void setSkin(PlayerSkinUtil.SkinSnapshot snapshot) {
    ResourceLocation texture = snapshot == null ? DEFAULT_TEXTURE : snapshot.texture();
    String model =
        snapshot == null || snapshot.model() == null
            ? PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT
            : snapshot.model();
    float red = snapshot == null ? 1.0f : snapshot.red();
    float green = snapshot == null ? 1.0f : snapshot.green();
    float blue = snapshot == null ? 1.0f : snapshot.blue();
    float alpha = snapshot == null ? 0.5f : snapshot.alpha();
    this.entityData.set(SKIN_TEXTURE, texture.toString());
    this.entityData.set(SKIN_MODEL, model);
    this.entityData.set(SKIN_COLOR, packColor(red, green, blue, alpha));
  }

  public ResourceLocation getSkinTexture() {
    String raw = this.entityData.get(SKIN_TEXTURE);
    ResourceLocation parsed = ResourceLocation.tryParse(raw);
    return parsed == null ? DEFAULT_TEXTURE : parsed;
  }

  public String getSkinModel() {
    String model = this.entityData.get(SKIN_MODEL);
    return model == null || model.isBlank() ? PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT : model;
  }

  public float[] getTintComponents() {
    int argb = this.entityData.get(SKIN_COLOR);
    float red = ((argb >> 16) & 0xFF) / 255.0f;
    float green = ((argb >> 8) & 0xFF) / 255.0f;
    float blue = (argb & 0xFF) / 255.0f;
    float alpha = ((argb >> 24) & 0xFF) / 255.0f;
    return new float[] {red, green, blue, alpha};
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    tag.putString("SkinTexture", this.entityData.get(SKIN_TEXTURE));
    tag.putString("SkinModel", this.entityData.get(SKIN_MODEL));
    tag.putInt("SkinColor", this.entityData.get(SKIN_COLOR));
    tag.putBoolean("SpawnHooks", this.spawnHookDispatched);
    if (templateId != null) {
      tag.putString("HunDaoTemplateId", templateId.toString());
    }
    tag.putInt("HunDaoLifetimeTicks", this.lifetimeTicks);
    ensureResourceState().save(tag);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.hasUUID("OwnerUUID")) {
      setOwnerUUID(tag.getUUID("OwnerUUID"));
    }
    if (tag.contains("SkinTexture")) {
      this.entityData.set(SKIN_TEXTURE, tag.getString("SkinTexture"));
    }
    if (tag.contains("SkinModel")) {
      this.entityData.set(SKIN_MODEL, tag.getString("SkinModel"));
    }
    if (tag.contains("SkinColor")) {
      this.entityData.set(SKIN_COLOR, tag.getInt("SkinColor"));
    }
    this.spawnHookDispatched = tag.getBoolean("SpawnHooks");
    if (tag.contains("HunDaoTemplateId")) {
      this.templateId = ResourceLocation.tryParse(tag.getString("HunDaoTemplateId"));
    } else {
      this.templateId = DEFAULT_TEMPLATE_ID;
    }
    if (tag.contains("HunDaoLifetimeTicks")) {
      this.lifetimeTicks = tag.getInt("HunDaoLifetimeTicks");
    } else {
      // 缺省为永久存在，避免重载后被默认 0 tick 判定移除
      this.lifetimeTicks = -1;
    }
    ensureResourceState().load(tag);
    this.refreshDimensions();
  }

  /**
   * 每 1000 点魂魄上限提供 +1 倍缩放（含 HitBox/攻击力）。
   */
  public double getHunpoScaleMultiplier() {
    double steps =
        Math.floor(Math.max(0.0D, ensureResourceState().getMaxHunpo()) / HUNPO_PER_SCALE_STEP);
    return 1.0D + steps;
  }

  public double getHunpoAttackMultiplier() {
    return getHunpoScaleMultiplier();
  }

  private HunDaoSoulAvatarResourceState ensureResourceState() {
    if (this.resourceState == null) {
      this.resourceState = new HunDaoSoulAvatarResourceState();
    }
    return this.resourceState;
  }

  @Override
  public boolean isAlliedTo(Entity entity) {
    if (entity == null) {
      return false;
    }
    if (entity == getOwner()) {
      return true;
    }
    return super.isAlliedTo(entity);
  }

  public boolean isOwnedBy(LivingEntity entity) {
    if (entity == null) {
      return false;
    }
    LivingEntity owner = getOwner();
    return owner != null && owner.equals(entity);
  }

  @Override
  public boolean canAttack(LivingEntity target) {
    if (target != null && target.equals(getOwner())) {
      return false;
    }
    return super.canAttack(target);
  }

  @Nullable
  @Override
  public LivingEntity getOwner() {
    Optional<UUID> id = this.entityData.get(OWNER);
    if (id.isEmpty()) {
      return null;
    }
    UUID uuid = id.get();
    if (uuid == null) {
      return null;
    }
    Entity ownerEntity = null;
    if (level() instanceof ServerLevel serverLevel) {
      ownerEntity = serverLevel.getEntity(uuid);
    }
    if (ownerEntity == null) {
      ownerEntity = level().getPlayerByUUID(uuid);
    }
    return ownerEntity instanceof LivingEntity living ? living : null;
  }

  @Nullable
  @Override
  public UUID getOwnerUUID() {
    return this.entityData.get(OWNER).orElse(null);
  }

  public void setOwnerUUID(@Nullable UUID ownerUUID) {
    if (ownerUUID == null) {
      this.entityData.set(OWNER, Optional.empty());
    } else {
      this.entityData.set(OWNER, Optional.of(ownerUUID));
    }
  }

  public ResourceLocation getTemplateId() {
    return templateId == null ? DEFAULT_TEMPLATE_ID : templateId;
  }

  public void setTemplateId(@Nullable ResourceLocation id) {
    this.templateId = id == null ? DEFAULT_TEMPLATE_ID : id;
  }

  public int getLifetimeTicks() {
    return lifetimeTicks;
  }

  public void setLifetimeTicks(int ticks) {
    this.lifetimeTicks = ticks < -1 ? -1 : ticks;
  }

  private static int packColor(float red, float green, float blue, float alpha) {
    int a = (int) (alpha * 255.0f) & 0xFF;
    int r = (int) (red * 255.0f) & 0xFF;
    int g = (int) (green * 255.0f) & 0xFF;
    int b = (int) (blue * 255.0f) & 0xFF;
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  private static class FollowOwnerGoal extends Goal {
    private final HunDaoSoulAvatarEntity avatar;
    private final float startDistanceSq;
    private final float stopDistanceSq;

    FollowOwnerGoal(HunDaoSoulAvatarEntity avatar, float startDistance, float stopDistance) {
      this.avatar = avatar;
      this.startDistanceSq = startDistance * startDistance;
      this.stopDistanceSq = stopDistance * stopDistance;
    }

    @Override
    public boolean canUse() {
      LivingEntity owner = avatar.getOwner();
      if (owner == null) {
        return false;
      }
      double distanceSq = avatar.distanceToSqr(owner);
      return distanceSq > startDistanceSq;
    }

    @Override
    public boolean canContinueToUse() {
      LivingEntity owner = avatar.getOwner();
      if (owner == null) {
        return false;
      }
      double distanceSq = avatar.distanceToSqr(owner);
      return distanceSq > stopDistanceSq;
    }

    @Override
    public void tick() {
      LivingEntity owner = avatar.getOwner();
      if (owner == null) {
        return;
      }
      avatar.getNavigation().moveTo(owner, 1.1);
    }
  }
}
