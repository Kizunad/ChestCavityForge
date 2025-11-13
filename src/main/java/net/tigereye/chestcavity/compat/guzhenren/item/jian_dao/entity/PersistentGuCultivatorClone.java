package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import kizuna.guzhenren_event_ext.common.util.GuCultivatorPersistentUtil;

/**
 * 多重剑影蛊的持久化蛊修分身实体。
 *
 * <p>核心特性：
 * <ul>
 *   <li>复制玩家皮肤（带半透明效果）
 *   <li>境界低玩家一个转数（最低1转1阶段）
 *   <li>7格物品栏（6格蛊虫 + 1格增益物品）
 *   <li>基础蛊师AI（近战 + 蛊虫释放）
 *   <li>可召唤/召回（序列化到物品NBT）
 * </ul>
 */
public class PersistentGuCultivatorClone extends PathfinderMob {

    // ============ 同步数据 (EntityDataAccessor) ============
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(PersistentGuCultivatorClone.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SKIN_TEXTURE =
            SynchedEntityData.defineId(PersistentGuCultivatorClone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_MODEL =
            SynchedEntityData.defineId(PersistentGuCultivatorClone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SKIN_COLOR =
            SynchedEntityData.defineId(PersistentGuCultivatorClone.class, EntityDataSerializers.INT);

    // ============ 物品栏 (ItemHandler能力) ============
    private final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            // 标记需要保存 + 通知分身更新
            PersistentGuCultivatorClone.this.inventoryChanged = true;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot < 6) {
                // 槽位0-5: 仅允许蛊虫
                return stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("guzhenren", "guchong")));
            } else {
                // 槽位6: 仅允许增益物品（暂时允许所有，后续可通过注册表限制）
                // TODO: 实现 CloneBoostItemRegistry.isBoostItem(stack.getItem())
                return true;
            }
        }
    };
    private boolean inventoryChanged = false;

    // ============ AI状态 (PersistentData) ============
    // 由 GuCultivatorAIAdapter 管理，字段包括：
    // - "近战": boolean
    // - "蛊虫CD", "蛊虫1CD"..."蛊虫5CD": double
    // - "ai_initialized": boolean (标记是否已初始化)

    // ============ 分频AI计数器 ============
    private int aiTickCounter = 0;

    // ============ 默认值 ============
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/steve.png");
    private static final int DEFAULT_TINT = 0x80A0A0FF; // 半透明蓝色

    public PersistentGuCultivatorClone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoAi(false);
        this.setPersistenceRequired(true); // 防止消失：确保分身在所有者离线或远离时不会被移除
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
        // 优先级0-4: 基础生存行为
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));

        // 优先级5-7: 战斗行为
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new MoveTowardsTargetGoal(this, 0.9, 32.0f));

        // 优先级8-10: 跟随与闲逛
        this.goalSelector.addGoal(8, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));

        // 目标选择
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    /**
     * 跟随所有者的AI Goal
     */
    private static class FollowOwnerGoal extends Goal {
        private final PersistentGuCultivatorClone clone;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;

        public FollowOwnerGoal(PersistentGuCultivatorClone clone, double speedModifier, float stopDistance, float startDistance) {
            this.clone = clone;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null || !owner.isAlive()) {
                return false;
            }
            return clone.distanceToSqr(owner) > startDistance * startDistance;
        }

        @Override
        public void tick() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null) {
                return;
            }
            if (clone.distanceToSqr(owner) > stopDistance * stopDistance) {
                clone.getNavigation().moveTo(owner, speedModifier);
            } else {
                clone.getNavigation().stop();
            }
        }
    }

    /**
     * 所有者被攻击时反击的AI Goal
     */
    private static class OwnerHurtByTargetGoal extends Goal {
        private final PersistentGuCultivatorClone clone;

        public OwnerHurtByTargetGoal(PersistentGuCultivatorClone clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null) {
                return false;
            }
            LivingEntity lastHurtByMob = owner.getLastHurtByMob();
            return lastHurtByMob != null && lastHurtByMob.isAlive();
        }

        @Override
        public void start() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner != null) {
                clone.setTarget(owner.getLastHurtByMob());
            }
        }
    }

    /**
     * 所有者攻击目标时协助攻击的AI Goal
     */
    private static class OwnerHurtTargetGoal extends Goal {
        private final PersistentGuCultivatorClone clone;

        public OwnerHurtTargetGoal(PersistentGuCultivatorClone clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null) {
                return false;
            }
            LivingEntity lastHurtMob = owner.getLastHurtMob();
            return lastHurtMob != null && lastHurtMob.isAlive();
        }

        @Override
        public void start() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner != null) {
                clone.setTarget(owner.getLastHurtMob());
            }
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();

        if (!this.level().isClientSide) {
            // 初始化AI数据 (首次运行)
            if (!getPersistentData().contains("ai_initialized")) {
                try {
                    // TODO: 实现 GuCultivatorAIAdapter.initializeAIData(this);
                    getPersistentData().putBoolean("ai_initialized", true);
                } catch (Exception e) {
                    // 初始化失败也标记已尝试，避免循环
                    getPersistentData().putBoolean("ai_initialized", true);
                    ChestCavity.LOGGER.warn("分身AI初始化失败: {}", e.getMessage());
                }
            }

            // 每3 tick执行一次AI逻辑 (降低性能开销)
            if (++aiTickCounter >= 3) {
                aiTickCounter = 0;
                try {
                    // TODO: 实现 GuCultivatorAIAdapter.tickGuUsage(this, inventory);
                    // 暂时留空，等待 GuCultivatorAIAdapter 实现
                } catch (Exception e) {
                    // 静默失败，记录日志
                    ChestCavity.LOGGER.warn("分身AI执行失败: {}", e.getMessage());
                }
            }
        }
    }

    // ============ 能力系统 ============

    /**
     * 获取物品栏 (用于 AI 访问)
     */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ============ NBT序列化（区块保存） ============

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // 持久化所有者UUID
        this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));

        // 持久化物品栏 (7格)
        tag.put("Inventory", this.inventory.serializeNBT(this.registryAccess()));

        // 持久化皮肤
        tag.putString("SkinTexture", this.entityData.get(SKIN_TEXTURE));
        tag.putString("SkinModel", this.entityData.get(SKIN_MODEL));
        tag.putInt("SkinColor", this.entityData.get(SKIN_COLOR));

        // 境界/资源由 PersistentData 自动保存
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // 恢复所有者UUID
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER, Optional.of(tag.getUUID("OwnerUUID")));
        }

        // 恢复物品栏
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(this.registryAccess(), tag.getCompound("Inventory"));
        }

        // 恢复皮肤
        if (tag.contains("SkinTexture")) {
            this.entityData.set(SKIN_TEXTURE, tag.getString("SkinTexture"));
            this.entityData.set(SKIN_MODEL, tag.getString("SkinModel"));
            this.entityData.set(SKIN_COLOR, tag.getInt("SkinColor"));
        }
    }

    // ============ 专用物品序列化方法 (用于召回时保存到物品NBT) ============

    /**
     * 序列化到物品NBT (仅存储必要字段)
     * 不复用 addAdditionalSaveData，避免状态不一致
     */
    public CompoundTag serializeToItemNBT() {
        CompoundTag tag = new CompoundTag();

        // 1. 所有者UUID
        this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));

        // 2. 境界数据
        tag.putDouble("Tier", this.getPersistentData().getDouble("野外蛊师转数"));
        tag.putDouble("Stage", this.getPersistentData().getDouble("野外蛊师阶段"));

        // 3. 皮肤数据
        tag.putString("SkinTexture", this.entityData.get(SKIN_TEXTURE));
        tag.putString("SkinModel", this.entityData.get(SKIN_MODEL));
        tag.putInt("SkinColor", this.entityData.get(SKIN_COLOR));

        // 4. 物品栏 (7格)
        CompoundTag inventoryTag = this.inventory.serializeNBT(this.registryAccess());
        tag.put("Inventory", inventoryTag);

        // 5. 资源快照 (仅当前值，重新召唤时会重新初始化上限)
        try {
            var handle = GuzhenrenResourceBridge.open(this);
            handle.ifPresent(h -> {
                h.getZhenyuan().ifPresent(v -> tag.putDouble("Zhenyuan", v));
                h.getJingli().ifPresent(v -> tag.putDouble("Jingli", v));
                h.read("hunpo").ifPresent(v -> tag.putDouble("Hunpo", v));
            });
        } catch (Exception e) {
            // 静默失败，使用默认值
        }

        return tag;
    }

    /**
     * 从物品NBT反序列化 (在spawn后调用)
     */
    public void deserializeFromItemNBT(CompoundTag tag) {
        // 1. 所有者UUID
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER, Optional.of(tag.getUUID("OwnerUUID")));
        }

        // 2. 境界数据
        if (tag.contains("Tier") && tag.contains("Stage")) {
            int tier = (int) tag.getDouble("Tier");
            int stage = (int) tag.getDouble("Stage");
            GuCultivatorPersistentUtil.setTierAndStage(this, tier, stage);
        }

        // 3. 皮肤数据
        if (tag.contains("SkinTexture")) {
            this.entityData.set(SKIN_TEXTURE, tag.getString("SkinTexture"));
            this.entityData.set(SKIN_MODEL, tag.getString("SkinModel"));
            this.entityData.set(SKIN_COLOR, tag.getInt("SkinColor"));
        }

        // 4. 物品栏
        if (tag.contains("Inventory")) {
            this.inventory.deserializeNBT(this.registryAccess(), tag.getCompound("Inventory"));
        }

        // 5. 资源快照 (仅恢复当前值，上限已在spawn时初始化)
        try {
            var handle = GuzhenrenResourceBridge.open(this);
            handle.ifPresent(h -> {
                if (tag.contains("Zhenyuan")) h.setZhenyuan(tag.getDouble("Zhenyuan"));
                if (tag.contains("Jingli")) h.setJingli(tag.getDouble("Jingli"));
                if (tag.contains("Hunpo")) h.writeDouble("hunpo", tag.getDouble("Hunpo"));
            });
        } catch (Exception e) {
            // 静默失败
        }
    }

    // ============ 所有权管理 ============

    /**
     * 检查指定玩家是否拥有该分身
     */
    public boolean isOwnedBy(Player player) {
        Optional<UUID> ownerOpt = this.entityData.get(OWNER);
        return ownerOpt.isPresent() && ownerOpt.get().equals(player.getUUID());
    }

    /**
     * 设置所有者
     */
    private void setOwner(Player owner) {
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
    }

    /**
     * 获取所有者实体（泛化版本，支持玩家和其他实体）
     */
    @Nullable
    public LivingEntity getOwnerEntity() {
        if (!(this.level() instanceof ServerLevel server)) {
            return null;
        }
        Optional<UUID> ownerId = this.entityData.get(OWNER);
        return ownerId
                .map(server::getEntity)
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .orElse(null);
    }

    /**
     * 打开物品栏界面 (带所有权校验)
     */
    public void openInventoryMenu(ServerPlayer player) {
        if (!isOwnedBy(player)) {
            player.displayClientMessage(Component.literal("§c这不是你的分身！"), true);
            return;
        }

        // TODO: 实现 CloneInventoryMenu
        player.displayClientMessage(Component.literal("§7分身界面尚未实现"), true);
        /*
        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> new CloneInventoryMenu(id, playerInv, this),
                Component.literal("分身物品栏")
        ));
        */
    }

    // ============ 皮肤管理 ============

    /**
     * 设置皮肤（从 SkinSnapshot）
     */
    private void setSkin(PlayerSkinUtil.SkinSnapshot snapshot) {
        // 同步到客户端
        int argb = ((int) (snapshot.alpha() * 255) & 0xFF) << 24
                | ((int) (snapshot.red() * 255) & 0xFF) << 16
                | ((int) (snapshot.green() * 255) & 0xFF) << 8
                | ((int) (snapshot.blue() * 255) & 0xFF);

        this.entityData.set(SKIN_COLOR, argb);
        this.entityData.set(SKIN_TEXTURE, snapshot.texture().toString());
        this.entityData.set(SKIN_MODEL, snapshot.model());
    }

    /**
     * 获取皮肤纹理
     */
    public ResourceLocation getSkinTexture() {
        String raw = this.entityData.get(SKIN_TEXTURE);
        ResourceLocation parsed = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        return parsed == null ? DEFAULT_TEXTURE : parsed;
    }

    /**
     * 获取皮肤模型（"default" 或 "slim"）
     */
    public String getSkinModel() {
        String model = this.entityData.get(SKIN_MODEL);
        return model == null || model.isBlank() ? PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT : model;
    }

    /**
     * 获取皮肤颜色组件（RGBA）
     */
    public float[] getTintComponents() {
        int argb = this.entityData.get(SKIN_COLOR);
        return new float[]{
                ((argb >> 16) & 0xFF) / 255.0f,  // Red
                ((argb >> 8) & 0xFF) / 255.0f,   // Green
                (argb & 0xFF) / 255.0f,          // Blue
                ((argb >> 24) & 0xFF) / 255.0f   // Alpha
        };
    }

    // ============ 死亡处理 ============

    @Override
    public void die(DamageSource source) {
        super.die(source);

        // TODO: 查找持有该分身的玩家并清理物品NBT
        // 这部分逻辑需要在 DuochongjianyingGuItem 中实现
        LivingEntity owner = getOwnerEntity();
        if (owner instanceof Player player) {
            player.displayClientMessage(Component.literal("§c你的分身已阵亡！"), true);
        }
    }

    // ============ 静态工厂方法 ============

    /**
     * 召唤新分身
     */
    public static PersistentGuCultivatorClone spawn(
            ServerLevel level,
            Player owner,
            Vec3 position
    ) {
        PersistentGuCultivatorClone clone = net.tigereye.chestcavity.registration.CCEntities.PERSISTENT_GU_CULTIVATOR_CLONE.get().create(level);
        if (clone == null) return null;

        clone.moveTo(position.x, position.y, position.z, owner.getYRot(), 0.0f);
        clone.setOwner(owner);

        // 1. 读取玩家转数
        int playerTier = 1;
        try {
            var handle = GuzhenrenResourceBridge.open(owner);
            if (handle.isPresent()) {
                playerTier = (int) Math.round(handle.get().getZhuanshu().orElse(1.0));
            }
        } catch (Exception e) {
            // 使用默认值
        }

        // 2. 计算分身境界: 玩家转数 - 1 (最低1转1阶段)
        int cloneTier = Math.max(1, playerTier - 1);
        int cloneStage = 1; // 初阶

        // 3. 设置皮肤（半透明蓝色）
        PlayerSkinUtil.SkinSnapshot skin = PlayerSkinUtil.capture(owner);
        PlayerSkinUtil.SkinSnapshot tintedSkin = PlayerSkinUtil.withTint(skin, 0.6f, 0.6f, 1.0f, 0.5f);
        clone.setSkin(tintedSkin);

        // 4. 设置境界
        GuCultivatorPersistentUtil.setTierAndStage(clone, cloneTier, cloneStage);

        // 5. 初始化资源 (基于转数)
        initializeResources(clone, cloneTier);

        // 6. 调用原生初始化 (触发模组AI钩子)
        try {
            if (clone instanceof Mob mob) {
                var diff = level.getCurrentDifficultyAt(mob.blockPosition());
                mob.finalizeSpawn(level, diff, MobSpawnType.MOB_SUMMONED, null);
            }
        } catch (Exception e) {
            ChestCavity.LOGGER.warn("finalizeSpawn失败: {}", e.getMessage());
        }

        level.addFreshEntity(clone);
        return clone;
    }

    /**
     * 初始化资源上限并顶满
     */
    private static void initializeResources(PersistentGuCultivatorClone clone, int tier) {
        try {
            var handle = GuzhenrenResourceBridge.open(clone);
            if (handle.isEmpty()) return;

            var h = handle.get();

            // 资源上限 (简化公式)
            double zhenyuanMax = tier * 200.0;
            double jingliMax = tier * 100.0;
            double hunpoMax = tier * 50.0;

            h.writeDouble("zuida_zhenyuan", zhenyuanMax);
            h.setZhenyuan(zhenyuanMax);

            h.writeDouble("zuida_jingli", jingliMax);
            h.setJingli(jingliMax);

            h.writeDouble("zuida_hunpo", hunpoMax);
            h.writeDouble("hunpo", hunpoMax);

        } catch (Exception e) {
            // 静默失败
        }
    }

    // ============ 属性配置 ============

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)         // 基础血量
                .add(Attributes.MOVEMENT_SPEED, 0.3)      // 移动速度
                .add(Attributes.ATTACK_DAMAGE, 2.0)       // 基础攻击力
                .add(Attributes.ARMOR, 2.0)               // 基础护甲
                .add(Attributes.FOLLOW_RANGE, 32.0);      // 跟随范围
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == this) {
            return true;
        }

        // 分身之间互相友好（同一所有者）
        if (entity instanceof PersistentGuCultivatorClone clone) {
            Optional<UUID> myOwner = this.entityData.get(OWNER);
            Optional<UUID> otherOwner = clone.entityData.get(OWNER);
            return myOwner.isPresent() && myOwner.equals(otherOwner);
        }

        // 与所有者及其盟友友好
        LivingEntity owner = getOwnerEntity();
        if (owner != null) {
            if (entity == owner) {
                return true;
            }
            if (entity instanceof LivingEntity living && living.isAlliedTo(owner)) {
                return true;
            }
        }

        return super.isAlliedTo(entity);
    }
}
