package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * 短效小光蛊幻象实体：复制玩家皮肤与装备，用于吸引火力。
 */
public class XiaoGuangIllusionEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> AURA_COLOR = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SKIN_TEXTURE = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_MODEL = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_URL = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_PROPERTY_VALUE = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_PROPERTY_SIGNATURE = SynchedEntityData.defineId(
            XiaoGuangIllusionEntity.class, EntityDataSerializers.STRING);

    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.parse("minecraft:textures/entity/steve.png");

    public XiaoGuangIllusionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(AURA_COLOR, 0xA0FFFFFF);
        builder.define(SKIN_TEXTURE, "");
        builder.define(SKIN_MODEL, PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
        builder.define(SKIN_URL, "");
        builder.define(SKIN_PROPERTY_VALUE, "");
        builder.define(SKIN_PROPERTY_SIGNATURE, "");
    }

    @Override
    protected void registerGoals() {
        // 无 AI
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 轻微漂浮：保持与生成方向一致
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 伤害在监听器中统一处理，这里直接忽略。
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // 幻象不可推挤
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    public void setOwner(@Nullable Player owner) {
        this.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
    }

    @Nullable
    public Player getOwner() {
        if (!(this.level() instanceof ServerLevel server)) {
            return null;
        }
        Optional<UUID> ownerId = this.entityData.get(OWNER);
        return ownerId.map(server::getPlayerByUUID).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        Optional<UUID> id = this.entityData.get(OWNER);
        return id.isPresent() && id.get().equals(player.getUUID());
    }

    public void setSkin(PlayerSkinUtil.SkinSnapshot snapshot) {
        ResourceLocation texture = snapshot.texture() != null ? snapshot.texture() : DEFAULT_TEXTURE;
        int argb = ((int) (snapshot.alpha() * 255.0f) & 0xFF) << 24
                | ((int) (snapshot.red() * 255.0f) & 0xFF) << 16
                | ((int) (snapshot.green() * 255.0f) & 0xFF) << 8
                | ((int) (snapshot.blue() * 255.0f) & 0xFF);
        this.entityData.set(AURA_COLOR, argb);
        this.entityData.set(SKIN_TEXTURE, texture.toString());
        this.entityData.set(SKIN_MODEL, snapshot.model());
        this.entityData.set(SKIN_URL, snapshot.skinUrl() == null ? "" : snapshot.skinUrl());
        this.entityData.set(SKIN_PROPERTY_VALUE, snapshot.propertyValue() == null ? "" : snapshot.propertyValue());
        this.entityData.set(SKIN_PROPERTY_SIGNATURE, snapshot.propertySignature() == null ? "" : snapshot.propertySignature());
    }

    public ResourceLocation getSkinTexture() {
        String raw = this.entityData.get(SKIN_TEXTURE);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TEXTURE;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        return parsed == null ? DEFAULT_TEXTURE : parsed;
    }

    public String getSkinModel() {
        String model = this.entityData.get(SKIN_MODEL);
        return model == null || model.isBlank() ? PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT : model;
    }

    public String getSkinUrl() {
        return this.entityData.get(SKIN_URL);
    }

    public String getSkinPropertyValue() {
        return this.entityData.get(SKIN_PROPERTY_VALUE);
    }

    public String getSkinPropertySignature() {
        return this.entityData.get(SKIN_PROPERTY_SIGNATURE);
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER);
    }

    public float[] getAuraColorComponents() {
        int argb = this.entityData.get(AURA_COLOR);
        return new float[]{
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >> 24) & 0xFF) / 255.0f
        };
    }

    public void copyEquipment(Player player) {
        if (player == null) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.setItemSlot(slot, stack.copyWithCount(1));
            }
        }
    }

    public void syncRotation(Player player) {
        if (player == null) {
            return;
        }
        this.setYRot(player.getYRot());
        this.setYHeadRot(player.getYHeadRot());
        this.setXRot(player.getXRot());
        this.yBodyRot = player.yBodyRot;
    }

    public void disperse(ServerLevel level) {
        Vec3 pos = this.position();
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.4f, 1.6f);
        level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y + 0.2, pos.z, 8, 0.2, 0.3, 0.2, 0.01);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.6, pos.z, 12, 0.25, 0.35, 0.25, 0.05);
        this.discard();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        }
        this.entityData.set(AURA_COLOR, tag.getInt("AuraColor"));
        if (tag.contains("SkinTexture")) {
            this.entityData.set(SKIN_TEXTURE, tag.getString("SkinTexture"));
        }
        if (tag.contains("SkinModel")) {
            this.entityData.set(SKIN_MODEL, tag.getString("SkinModel"));
        }
        if (tag.contains("SkinUrl")) {
            this.entityData.set(SKIN_URL, tag.getString("SkinUrl"));
        }
        if (tag.contains("SkinProperty")) {
            this.entityData.set(SKIN_PROPERTY_VALUE, tag.getString("SkinProperty"));
        }
        if (tag.contains("SkinSignature")) {
            this.entityData.set(SKIN_PROPERTY_SIGNATURE, tag.getString("SkinSignature"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("AuraColor", this.entityData.get(AURA_COLOR));
        tag.putString("SkinTexture", this.entityData.get(SKIN_TEXTURE));
        tag.putString("SkinModel", this.entityData.get(SKIN_MODEL));
        String skinUrl = this.entityData.get(SKIN_URL);
        if (skinUrl != null && !skinUrl.isBlank()) {
            tag.putString("SkinUrl", skinUrl);
        }
        String propertyValue = this.entityData.get(SKIN_PROPERTY_VALUE);
        if (propertyValue != null && !propertyValue.isBlank()) {
            tag.putString("SkinProperty", propertyValue);
        }
        String propertySignature = this.entityData.get(SKIN_PROPERTY_SIGNATURE);
        if (propertySignature != null && !propertySignature.isBlank()) {
            tag.putString("SkinSignature", propertySignature);
        }
    }

    public static XiaoGuangIllusionEntity create(ServerLevel level, Player owner, Vec3 spawnPos, PlayerSkinUtil.SkinSnapshot skin) {
        XiaoGuangIllusionEntity illusion = net.tigereye.chestcavity.registration.CCEntities.XIAO_GUANG_ILLUSION.get().create(level);
        if (illusion == null) {
            return null;
        }
        illusion.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, owner.getYRot(), owner.getXRot());
        illusion.setOwner(owner);
        illusion.syncRotation(owner);
        illusion.copyEquipment(owner);
        illusion.setSkin(skin);
        illusion.setGlowingTag(true);
        return illusion;
    }
}
