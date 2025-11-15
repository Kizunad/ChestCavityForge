package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.CloneBoostItemRegistry;
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
            // 物品栏变化：标记为脏，下一tick立即同步装备
            inventoryDirty = true;
            // 槽6为增益物品，变化时立即应用/移除效果
            if (slot == 6 && !level().isClientSide) {
                try {
                    PersistentGuCultivatorClone.this.updateBoostEffect();
                } catch (Exception e) {
                    ChestCavity.LOGGER.warn("增益槽更新失败: {}", e.getMessage());
                }
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 6 ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 新规则：
            // 0 主手：允许任意物品
            // 1 副手：允许食物、金苹果/附魔金苹果、图腾
            // 2..5 盔甲：要求 ArmorItem 且槽位匹配
            // 6 增益：暂时允许所有（由增益注册表约束实际效果）
            if (slot == 0) {
                return !stack.isEmpty();
            }
            if (slot == 1) {
                boolean golden = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
                boolean totem = stack.is(Items.TOTEM_OF_UNDYING);
                boolean edible = stack.getItem().getFoodProperties(stack, PersistentGuCultivatorClone.this) != null;
                return golden || totem || edible;
            }
            if (slot >= 2 && slot <= 5) {
                EquipmentSlot eq = switch (slot) {
                    case 2 -> EquipmentSlot.HEAD;
                    case 3 -> EquipmentSlot.CHEST;
                    case 4 -> EquipmentSlot.LEGS;
                    default -> EquipmentSlot.FEET;
                };
                return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == eq;
            }
            // 槽位6：增益
            return true;
        }
    };

    // ============ AI状态 (PersistentData) ============
    // 由 GuCultivatorAIAdapter 管理，字段包括：
    // - "近战": boolean
    // - "蛊虫CD", "蛊虫1CD"..."蛊虫5CD": double
    // - "ai_initialized": boolean (标记是否已初始化)

    // （移除）分频AI计数器：寻路/Goal 每tick由引擎调度，无需自节流

    // ============ 装备同步/副手消耗控制 ============
    private int equipSyncCounter = 0; // 分频同步装备
    private static final int EQUIP_SYNC_INTERVAL_TICKS = 20; // 每秒同步一次
    private static final String KEY_OFFHAND_CONSUME_CD = "offhand_consume_cd";
    private static final double OFFHAND_CONSUME_COOLDOWN_TICKS = 100.0; // 5秒CD
    private boolean inventoryDirty = false; // 物品栏变更标记

    // ============ 增益效果追踪 ============
    private ItemStack currentBoostItem = ItemStack.EMPTY;

    // ============ 默认值 ============
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/steve.png");
    private static final int DEFAULT_TINT = 0x80A0A0FF; // 半透明蓝色
    private static final double BASE_MAX_HEALTH = 20.0;
    private static final double BASE_ATTACK_DAMAGE = 2.0;
    private static final ResourceLocation HEALTH_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "clone_health_bonus");
    private static final ResourceLocation ATTACK_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "clone_attack_bonus");

    public PersistentGuCultivatorClone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setNoAi(false);
        this.setPersistenceRequired(); // 防止消失：确保分身在所有者离线或远离时不会被移除
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
        // 优先级5: 近战（自带接近目标逻辑）
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));

        // 优先级8-10: 跟随与闲逛
        this.goalSelector.addGoal(8, new FollowOwnerGoal(this, 10.0f, 2.0f));
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
        private final float startDistanceSq;
        private final float stopDistanceSq;

        public FollowOwnerGoal(PersistentGuCultivatorClone clone, float startDistance, float stopDistance) {
            this.clone = clone;
            this.startDistanceSq = startDistance * startDistance;
            this.stopDistanceSq = stopDistance * stopDistance;
        }

        @Override
        public boolean canUse() {
            if (clone.getTarget() != null) {
                return false;
            }
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null || !owner.isAlive()) {
                return false;
            }
            return clone.distanceToSqr(owner) > startDistanceSq;
        }

        @Override
        public void tick() {
            LivingEntity owner = clone.getOwnerEntity();
            if (owner == null || clone.getTarget() != null) {
                return;
            }
            double distSq = clone.distanceToSqr(owner);
            if (distSq > startDistanceSq) {
                clone.getNavigation().moveTo(owner, 1.0);
            } else if (distSq < stopDistanceSq) {
                clone.getNavigation().stop();
            }
        }
    }

    // 使用默认 MeleeAttackGoal，无需额外包装

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
            // 关闭原蛊虫AI：不再初始化/执行 GuCultivatorAIAdapter

            // 物品栏变更即刻同步（主手/副手/盔甲）
            if (inventoryDirty) {
                inventoryDirty = false;
                try {
                    syncEquipmentFromInventory();
                } catch (Exception e) {
                    ChestCavity.LOGGER.warn("分身装备同步失败: {}", e.getMessage());
                }
            }

            // 定期兜底同步（防止漏同步）
            if (++equipSyncCounter >= EQUIP_SYNC_INTERVAL_TICKS) {
                equipSyncCounter = 0;
                try {
                    syncEquipmentFromInventory();
                } catch (Exception e) {
                    ChestCavity.LOGGER.warn("分身装备同步失败: {}", e.getMessage());
                }
            }

            // 副手自动进食/金苹果消耗（满血不消耗，内置CD）
            try {
                tickOffhandAutoConsume();
            } catch (Exception e) {
                ChestCavity.LOGGER.warn("副手消耗处理失败: {}", e.getMessage());
            }

        }
    }

    /**
     * 处理玩家与分身的交互。
     *
     * <p><strong>交互逻辑：</strong>
     * <ul>
     *   <li>空手 + Shift + 右键 → 打开分身管理界面
     *   <li>其他情况 → 不处理，传递给父类
     * </ul>
     *
     * @param player 与分身交互的玩家
     * @param hand 玩家使用的手（主手/副手）
     * @return 交互结果
     */
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (this.level().isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 检查是否为空手 Shift+右键
        ItemStack handItem = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && handItem.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                openInventoryMenu(serverPlayer);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }

        return net.minecraft.world.InteractionResult.PASS;
    }

    // ============ 能力系统 ============

    /**
     * 获取物品栏 (用于 AI 访问、GUI和Capability系统)
     *
     * 注意：在 NeoForge 1.21.1 中，Entity.getCapability() 是 final 方法，无法覆盖。
     * 能力系统通过 RegisterCapabilitiesEvent 注册，在 ChestCavity.registerCapabilities() 中。
     * 外部代码可以通过 entity.getCapability(Capabilities.ItemHandler.ENTITY) 访问此物品栏。
     */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ============ 增益效果管理 ============

    /**
     * 更新增益效果
     *
     * <p>检查增益槽位（槽位6）的物品是否变化，如果变化则：
     * <ul>
     *   <li>移除旧的增益效果
     *   <li>应用新的增益效果
     * </ul>
     *
     * <p><strong>调用时机：</strong>
     * <ul>
     *   <li>GUI中增益槽位变化时（由 {@link net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu} 调用）
     *   <li>分身从物品NBT恢复后
     * </ul>
     */
    public void updateBoostEffect() {
        if (this.level().isClientSide) {
            return; // 仅在服务端处理
        }

        ItemStack newBoostItem = this.inventory.getStackInSlot(6);

        // 检查增益物品是否变化
        if (ItemStack.matches(currentBoostItem, newBoostItem)) {
            return; // 没有变化，不需要更新
        }

        // 移除旧的增益效果
        if (!currentBoostItem.isEmpty()) {
            CloneBoostItemRegistry.getBoostEffect(currentBoostItem.getItem())
                    .ifPresent(effect -> {
                        try {
                            effect.remove(this, currentBoostItem);
                        } catch (Exception e) {
                            ChestCavity.LOGGER.warn("移除增益效果失败: {}", e.getMessage());
                        }
                    });
        }

        // 应用新的增益效果
        if (!newBoostItem.isEmpty()) {
            CloneBoostItemRegistry.getBoostEffect(newBoostItem.getItem())
                    .ifPresent(effect -> {
                        try {
                            effect.apply(this, newBoostItem);
                        } catch (Exception e) {
                            ChestCavity.LOGGER.warn("应用增益效果失败: {}", e.getMessage());
                        }
                    });
        }

        // 更新追踪的增益物品
        currentBoostItem = newBoostItem.copy();
    }

    /**
     * 应用增益效果（用于物品恢复后）
     *
     * <p>仅应用效果，不移除旧效果。用于分身首次生成或从NBT恢复后。
     */
    public void applyBoostEffect() {
        if (this.level().isClientSide) {
            return;
        }

        ItemStack boostItem = this.inventory.getStackInSlot(6);
        if (!boostItem.isEmpty()) {
            CloneBoostItemRegistry.getBoostEffect(boostItem.getItem())
                    .ifPresent(effect -> {
                        try {
                            effect.apply(this, boostItem);
                            currentBoostItem = boostItem.copy();
                        } catch (Exception e) {
                            ChestCavity.LOGGER.warn("应用增益效果失败: {}", e.getMessage());
                        }
                    });
        }
    }

    /**
     * 移除所有增益效果（用于分身召回或死亡前）
     */
    public void removeBoostEffect() {
        if (this.level().isClientSide) {
            return;
        }

        if (!currentBoostItem.isEmpty()) {
            CloneBoostItemRegistry.getBoostEffect(currentBoostItem.getItem())
                    .ifPresent(effect -> {
                        try {
                            effect.remove(this, currentBoostItem);
                        } catch (Exception e) {
                            ChestCavity.LOGGER.warn("移除增益效果失败: {}", e.getMessage());
                        }
                    });
            currentBoostItem = ItemStack.EMPTY;
        }
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

        // 6. 分身自身的胸腔数据 (Entity ChestCavity)
        try {
            var chestCavity = net.tigereye.chestcavity.registration.CCAttachments.getChestCavity(this);
            if (chestCavity != null && chestCavity.opened) {
                CompoundTag ccWrapper = new CompoundTag();
                chestCavity.toTag(ccWrapper, this.registryAccess());
                // 仅在胸腔已打开时保存数据
                if (ccWrapper.contains("ChestCavity")) {
                    tag.put("EntityChestCavity", ccWrapper.getCompound("ChestCavity"));
                }
            }
        } catch (Exception e) {
            // 静默失败，胸腔数据丢失但不影响其他功能
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

        // 6. 分身自身的胸腔数据恢复 (Entity ChestCavity)
        try {
            if (tag.contains("EntityChestCavity")) {
                var chestCavity = net.tigereye.chestcavity.registration.CCAttachments.getChestCavity(this);
                if (chestCavity != null) {
                    // 构造包装Tag (ChestCavity系统需要 "ChestCavity" 键)
                    CompoundTag ccWrapper = new CompoundTag();
                    ccWrapper.put("ChestCavity", tag.getCompound("EntityChestCavity"));
                    chestCavity.fromTag(ccWrapper, this, this.registryAccess());
                }
            }
        } catch (Exception e) {
            // 静默失败，胸腔数据丢失但不影响其他功能
        }

        // 7. 应用增益效果（恢复后）
        applyBoostEffect();
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

        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu(id, playerInv, this),
                Component.literal("分身物品栏")
        ));
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
        // 移除增益效果（在死亡前）
        removeBoostEffect();

        super.die(source);

        // TODO: 查找持有该分身的玩家并清理物品NBT
        // 这部分逻辑需要在 DuochongjianyingGuItem 中实现
        LivingEntity owner = getOwnerEntity();
        if (owner instanceof Player player) {
            player.displayClientMessage(Component.literal("§c你的分身已阵亡！"), true);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result) {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        return result;
    }

    // ============ 装备同步与副手消耗 ============

    /**
     * 将物品栏槽位 0..5 同步到实体装备：
     * 0→主手，1→副手，2→头盔，3→胸甲，4→护腿，5→靴子；6为增益物品不参与。
     * 盔甲位仅在物品为 ArmorItem 且槽位匹配时同步。
     */
    private void syncEquipmentFromInventory() {
        // 主手/副手
        ItemStack main = inventory.getStackInSlot(0);
        ItemStack off = inventory.getStackInSlot(1);
        if (!(ItemStack.isSameItemSameComponents(main, this.getMainHandItem()) && main.getCount() == this.getMainHandItem().getCount())) {
            this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main.copy());
        }
        if (!(ItemStack.isSameItemSameComponents(off, this.getOffhandItem()) && off.getCount() == this.getOffhandItem().getCount())) {
            this.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off.copy());
        }

        // 盔甲：头、胸、腿、足
        syncArmorSlot(2, EquipmentSlot.HEAD);
        syncArmorSlot(3, EquipmentSlot.CHEST);
        syncArmorSlot(4, EquipmentSlot.LEGS);
        syncArmorSlot(5, EquipmentSlot.FEET);
    }

    private void syncArmorSlot(int invSlot, EquipmentSlot slot) {
        ItemStack want = inventory.getStackInSlot(invSlot);
        ItemStack worn = this.getItemBySlot(slot);

        if (want.isEmpty()) {
            if (!worn.isEmpty()) this.setItemSlot(slot, ItemStack.EMPTY);
            return;
        }

        if (want.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot) {
            if (!(ItemStack.isSameItemSameComponents(want, worn) && want.getCount() == worn.getCount())) {
                this.setItemSlot(slot, want.copy());
            }
        } else {
            // 非法物品不强制覆盖盔甲位
        }
    }

    /**
     * 副手自动进食/金苹果：
     * - 满血不消耗；
     * - 非满血且物品可食用或为金苹果/附魔金苹果时，消耗一枚并应用效果；
     * - 内置5秒CD，使用 PersistentData 键 KEY_OFFHAND_CONSUME_CD 控制。
     */
    private void tickOffhandAutoConsume() {
        var tag = this.getPersistentData();
        tag.putDouble(KEY_OFFHAND_CONSUME_CD, tag.getDouble(KEY_OFFHAND_CONSUME_CD) - 1.0);

        if (this.getHealth() >= this.getMaxHealth()) {
            return; // 满血不消耗
        }
        if (tag.getDouble(KEY_OFFHAND_CONSUME_CD) > 0.0) {
            return; // 冷却中
        }

        ItemStack slotOff = inventory.getStackInSlot(1);
        if (slotOff.isEmpty()) return;

        boolean isGoldenApple = slotOff.is(Items.GOLDEN_APPLE) || slotOff.is(Items.ENCHANTED_GOLDEN_APPLE);
        FoodProperties fp0 = slotOff.getItem().getFoodProperties(slotOff, this);
        boolean isEdible = fp0 != null;
        if (!isGoldenApple && !isEdible) return;

        // 应用吃下效果
        if (isGoldenApple) {
            // 直接调用 finishUsingItem 以应用金苹果完整效果
            ItemStack tmp = slotOff.copy();
            tmp.setCount(1);
            tmp.getItem().finishUsingItem(tmp, this.level(), this);
            // 消耗1个
            slotOff.shrink(1);
            inventory.setStackInSlot(1, slotOff);
            // 立即反映到副手
            this.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, slotOff.copy());

            // 反馈特效（可见性/声音）
            if (this.level() instanceof ServerLevel s) {
                s.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + this.getBbHeight() * 0.5,
                        this.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                s.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.0f);
            }
        } else if (isEdible) {
            // 普通食物：按营养值转化为治疗量（每点营养=1点生命），并消耗1个
            int nutrition = fp0.nutrition();
            if (nutrition > 0) {
                this.heal(Math.min(nutrition, (int) (this.getMaxHealth() - this.getHealth())));
            }
            slotOff.shrink(1);
            inventory.setStackInSlot(1, slotOff);
            this.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, slotOff.copy());
            if (this.level() instanceof ServerLevel s) {
                s.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + this.getBbHeight() * 0.5,
                        this.getZ(), 4, 0.25, 0.3, 0.25, 0.05);
                s.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6f, 1.2f);
            }
        }

        // 冷却重置
        tag.putDouble(KEY_OFFHAND_CONSUME_CD, OFFHAND_CONSUME_COOLDOWN_TICKS);
        // 标记脏位，保证后续兜底同步
        inventoryDirty = true;
    }

    // ============ 自定义不死图腾效果 ============
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }

        // 预判致命伤害并尝试触发不死图腾（主手/副手任一持有）
        float remaining = this.getHealth() - amount;
        if (remaining <= 0.0f && tryUseTotemLikeEffect()) {
            // 取消这次伤害
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean tryUseTotemLikeEffect() {
        ItemStack main = this.getMainHandItem();
        ItemStack off = this.getOffhandItem();
        boolean hasTotem = main.is(Items.TOTEM_OF_UNDYING) || off.is(Items.TOTEM_OF_UNDYING);
        if (!hasTotem) return false;

        // 消耗一个
        if (off.is(Items.TOTEM_OF_UNDYING)) {
            off.shrink(1);
            this.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
        } else {
            main.shrink(1);
            this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main);
        }

        // 复活与效果（参考原版：生命值重置+多种增益）
        this.setHealth(Math.max(1.0f, this.getMaxHealth() * 0.2f));
        this.removeAllEffects();
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        this.invulnerableTime = 20; // 短暂无敌窗口

        if (this.level() instanceof ServerLevel s) {
            s.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + this.getBbHeight() * 0.5,
                    this.getZ(), 16, 0.5, 0.6, 0.5, 0.1);
            s.playSound(null, this.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        return true;
    }

    

    // ============ 静态工厂方法 ============

    /**
     * 召唤新分身
     */
    public static PersistentGuCultivatorClone spawn(
            ServerLevel level,
            Player owner,
            Vec3 position,
            double attributeMultiplier
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

        // 5.1 根据剑道道痕调整基础属性
        applyAttributeScaling(clone, owner, attributeMultiplier);

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

    private static void applyAttributeScaling(
            PersistentGuCultivatorClone clone, Player owner, double multiplier) {
        double bonusFactor = Math.max(0.0, multiplier - 1.0);

        AttributeInstance healthAttr = clone.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double ownerHealth = owner.getAttributeValue(Attributes.MAX_HEALTH);
            double bonusFromOwner = Math.max(0.0, ownerHealth - BASE_MAX_HEALTH);
            double bonus = BASE_MAX_HEALTH * bonusFactor + bonusFromOwner;
            applyAttributeBonus(healthAttr, HEALTH_BONUS_ID, "jiandao.daohen.health", bonus);
            clone.setHealth((float) healthAttr.getValue());
        }

        AttributeInstance attackAttr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double ownerAttack = owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double bonusFromOwner = Math.max(0.0, ownerAttack - BASE_ATTACK_DAMAGE);
            double bonus = BASE_ATTACK_DAMAGE * bonusFactor + bonusFromOwner;
            applyAttributeBonus(attackAttr, ATTACK_BONUS_ID, "jiandao.daohen.attack", bonus);
        }
    }

    private static void applyAttributeBonus(
            AttributeInstance instance, ResourceLocation id, String name, double amount) {
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount <= 0.0) {
            return;
        }
        AttributeModifier modifier =
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
        instance.addTransientModifier(modifier);
    }

    // ============ 属性配置 ============

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)         // 基础血量
                .add(Attributes.MOVEMENT_SPEED, 0.3)      // 移动速度
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)       // 基础攻击力
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
