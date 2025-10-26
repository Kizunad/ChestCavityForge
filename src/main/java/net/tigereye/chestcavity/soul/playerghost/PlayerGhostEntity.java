package net.tigereye.chestcavity.soul.playerghost;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;

/**
 * 玩家幽灵实体 - 普通敌对生物
 *
 * <p>职责：
 * - 继承 Monster，作为普通敌对生物
 * - 使用玩家皮肤渲染
 * - 攻击所有玩家
 * - 保留原玩家的装备和属性
 */
public class PlayerGhostEntity extends Monster {

  // 同步实体数据：皮肤纹理和模型
  private static final EntityDataAccessor<String> SKIN_TEXTURE =
      SynchedEntityData.defineId(PlayerGhostEntity.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<String> SKIN_MODEL =
      SynchedEntityData.defineId(PlayerGhostEntity.class, EntityDataSerializers.STRING);

  // 默认史蒂夫皮肤
  private static final ResourceLocation DEFAULT_TEXTURE =
      ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

  public PlayerGhostEntity(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }

  /**
   * 从玩家死亡记录创建幽灵
   */
  public static PlayerGhostEntity createFromArchive(
      EntityType<PlayerGhostEntity> type, Level level, PlayerGhostArchive archive) {
    PlayerGhostEntity ghost = new PlayerGhostEntity(type, level);

    // 设置自定义名称
    String displayName = archive.getPlayerName() + "的魂魄";
    ghost.setCustomName(Component.literal(displayName));
    ghost.setCustomNameVisible(true);

    // 应用皮肤
    ghost.applySkin(archive.getSkinTexture(), archive.getSkinModel());

    // 恢复装备
    ghost.setItemSlot(EquipmentSlot.HEAD, archive.getHelmet());
    ghost.setItemSlot(EquipmentSlot.CHEST, archive.getChestplate());
    ghost.setItemSlot(EquipmentSlot.LEGS, archive.getLeggings());
    ghost.setItemSlot(EquipmentSlot.FEET, archive.getBoots());
    ghost.setItemSlot(EquipmentSlot.MAINHAND, archive.getMainHand());
    ghost.setItemSlot(EquipmentSlot.OFFHAND, archive.getOffHand());

    // 设置所有装备不掉落
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      ghost.setDropChance(slot, 0.0f);
    }

    // 应用属性
    ghost.applyArchiveAttributes(archive);

    return ghost;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(SKIN_TEXTURE, DEFAULT_TEXTURE.toString());
    builder.define(SKIN_MODEL, PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
  }

  @Override
  protected void registerGoals() {
    // 基本移动 AI
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false)); // 攻击时速度稍快
    this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0)); // 闲逛时使用正常速度
    this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
    this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

    // 攻击所有玩家（敌对）
    this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 20.0)
        .add(Attributes.MOVEMENT_SPEED, 0.8) // 基础速度 0.8，会根据玩家HP进一步增加
        .add(Attributes.ATTACK_DAMAGE, 5.0)
        .add(Attributes.FOLLOW_RANGE, 32.0)
        .add(Attributes.ARMOR, 0.0)
        .add(Attributes.ARMOR_TOUGHNESS, 0.0)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
  }

  /**
   * 应用玩家皮肤
   */
  private void applySkin(String skinTexture, String skinModel) {
    if (skinTexture == null || skinTexture.isBlank()) {
      // 使用默认皮肤
      this.entityData.set(SKIN_TEXTURE, DEFAULT_TEXTURE.toString());
      this.entityData.set(SKIN_MODEL, PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
      return;
    }

    this.entityData.set(SKIN_TEXTURE, skinTexture);
    this.entityData.set(
        SKIN_MODEL,
        skinModel != null && !skinModel.isBlank()
            ? skinModel
            : PlayerSkinUtil.SkinSnapshot.MODEL_DEFAULT);
  }

  /**
   * 应用死亡记录中的属性
   */
  private void applyArchiveAttributes(PlayerGhostArchive archive) {
    try {
      // 设置生命值和生命上限
      if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(archive.getMaxHealth());
        this.setHealth((float) archive.getMaxHealth());
      }

      // 设置攻击力
      if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null && archive.getAttackDamage() > 0) {
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(archive.getAttackDamage());
      }

      // 设置护甲值
      if (this.getAttribute(Attributes.ARMOR) != null && archive.getArmor() > 0) {
        this.getAttribute(Attributes.ARMOR).setBaseValue(archive.getArmor());
      }

      // 设置护甲韧性
      if (this.getAttribute(Attributes.ARMOR_TOUGHNESS) != null
          && archive.getArmorToughness() > 0) {
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(archive.getArmorToughness());
      }

      // 设置移动速度：基础速度 0.8 + (玩家最大HP * 0.01%)
      if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
        double baseSpeed = 0.8;
        double hpBonus = archive.getMaxHealth() * 0.0001; // 0.01% of max HP
        double finalSpeed = baseSpeed + hpBonus;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(finalSpeed);

        ChestCavity.LOGGER.debug(
            "[PlayerGhost] 设置移动速度: 基础={} + HP加成={} = {}",
            baseSpeed, hpBonus, finalSpeed);
      }

      // 设置击退抗性
      if (this.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null
          && archive.getKnockbackResistance() > 0) {
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
            .setBaseValue(archive.getKnockbackResistance());
      }
    } catch (Exception e) {
      ChestCavity.LOGGER.warn("[PlayerGhost] 应用属性失败，使用默认属性", e);
    }
  }

  /**
   * 获取皮肤纹理位置
   */
  public ResourceLocation getSkinTexture() {
    String texture = this.entityData.get(SKIN_TEXTURE);
    try {
      return ResourceLocation.parse(texture);
    } catch (Exception e) {
      return DEFAULT_TEXTURE;
    }
  }

  /**
   * 获取皮肤模型类型（"default" 或 "slim"）
   */
  public String getSkinModel() {
    return this.entityData.get(SKIN_MODEL);
  }

  /**
   * 是否为纤细模型（Alex 型）
   */
  public boolean isSlimModel() {
    return "slim".equals(getSkinModel());
  }

  /**
   * 攻击目标时附加基于当前生命值的额外伤害
   *
   * <p>额外伤害 = 当前生命值 × 10%
   */
  @Override
  public boolean doHurtTarget(Entity target) {
    boolean base = super.doHurtTarget(target);
    if (base && target instanceof LivingEntity living) {
      // 附加当前生命值 10% 的额外伤害
      float extra = this.getHealth() * 0.1f;
      if (extra > 0f) {
        living.hurt(this.damageSources().mobAttack(this), extra);
      }
    }
    return base;
  }

  /**
   * 击杀生物后，将击杀目标的最大生命值100%转化为自身最大生命值增长
   *
   * <p>例如：击杀一个 20HP 的玩家，自身最大生命值增加 20HP
   */
  @Override
  public void awardKillScore(Entity entity, int score, DamageSource source) {
    super.awardKillScore(entity, score, source);
    if (entity instanceof LivingEntity living) {
      double gained = living.getMaxHealth();
      if (gained > 0.0) {
        var attribute = this.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
          // 增加最大生命值
          double newBase = attribute.getBaseValue() + gained;
          attribute.setBaseValue(newBase);
          // 恢复等量的生命值（但不超过新的最大值）
          this.setHealth((float) Math.min(attribute.getValue(), this.getHealth() + (float) gained));

          ChestCavity.LOGGER.debug(
              "[PlayerGhost] {} 击杀 {} 获得 {} 最大生命值，当前: {}/{}",
              this.getName().getString(),
              living.getName().getString(),
              gained,
              this.getHealth(),
              this.getMaxHealth());
        }
      }
    }
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString("skin_texture", this.entityData.get(SKIN_TEXTURE));
    tag.putString("skin_model", this.entityData.get(SKIN_MODEL));
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains("skin_texture")) {
      this.entityData.set(SKIN_TEXTURE, tag.getString("skin_texture"));
    }
    if (tag.contains("skin_model")) {
      this.entityData.set(SKIN_MODEL, tag.getString("skin_model"));
    }
  }

  /**
   * 死亡时给予击杀者（玩家）随机奖励
   */
  @Override
  public void die(DamageSource source) {
    super.die(source);

    // 只在服务端处理
    if (this.level().isClientSide) {
      return;
    }

    // 获取击杀者
    Entity killer = source.getEntity();
    if (!(killer instanceof Player player)) {
      return;
    }

    // 给予随机奖励
    grantKillReward(player);
  }

  /**
   * 给予击杀玩家幽灵的奖励
   *
   * @param player 击杀者
   */
  private void grantKillReward(Player player) {
    double ghostHP = this.getMaxHealth();

    // 获取幽灵名称（格式："XX的魂魄"）
    String ghostName = this.hasCustomName()
        ? this.getCustomName().getString()
        : "未知的魂魄";

    // 随机选择奖励类型
    RewardType reward = RewardType.randomReward(this.random);

    switch (reward) {
      case MAX_HEALTH -> grantMaxHealthReward(player, ghostHP, ghostName);
      case ATTACK_DAMAGE -> grantAttackDamageReward(player, ghostHP, ghostName);
      case MAX_HUNPO -> grantGuzhenrenReward(player, ghostHP, ghostName, "zuida_hunpo", "魂魄上限");
      case MAX_ZHENYUAN -> grantGuzhenrenReward(player, ghostHP, ghostName, "zuida_zhenyuan", "真元上限");
      case MAX_JINGLI -> grantGuzhenrenReward(player, ghostHP, ghostName, "zuida_jingli", "精力上限");
      case SHOUYUAN -> grantGuzhenrenReward(player, ghostHP, ghostName, "shouyuan", "寿元");
      case QIYUN -> grantGuzhenrenReward(player, ghostHP, ghostName, "qiyun", "气运");
      case RENQI -> grantGuzhenrenReward(player, ghostHP, ghostName, "renqi", "人气");
    }
  }

  /**
   * 给予最大生命值奖励
   */
  private void grantMaxHealthReward(Player player, double ghostHP, String ghostName) {
    // 随机 1% ~ 5% 的幽灵最大生命值
    double percentage = 0.01 + this.random.nextDouble() * 0.04;
    double amount = ghostHP * percentage;

    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      AttributeModifier modifier =
          new AttributeModifier(
              ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "ghost_kill_health"),
              amount,
              AttributeModifier.Operation.ADD_VALUE);
      maxHealth.addPermanentModifier(modifier);
      player.setHealth((float) Math.min(player.getMaxHealth(), player.getHealth() + (float) amount));

      player.sendSystemMessage(
          Component.literal(String.format("§6击杀了§r%s§6，获得了§c最大生命值+%.2f", ghostName, amount)));
      ChestCavity.LOGGER.info(
          "[PlayerGhost] {} 击杀 {} 获得最大生命值 +{}",
          player.getName().getString(), ghostName, amount);
    }
  }

  /**
   * 给予攻击力奖励
   */
  private void grantAttackDamageReward(Player player, double ghostHP, String ghostName) {
    // 随机 0.1% ~ 1% 的幽灵最大生命值
    double percentage = 0.001 + this.random.nextDouble() * 0.009;
    double amount = ghostHP * percentage;

    AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attackDamage != null) {
      AttributeModifier modifier =
          new AttributeModifier(
              ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "ghost_kill_attack"),
              amount,
              AttributeModifier.Operation.ADD_VALUE);
      attackDamage.addPermanentModifier(modifier);

      player.sendSystemMessage(
          Component.literal(String.format("§6击杀了§r%s§6，获得了§c基础攻击力+%.2f", ghostName, amount)));
      ChestCavity.LOGGER.info(
          "[PlayerGhost] {} 击杀 {} 获得基础攻击力 +{}",
          player.getName().getString(), ghostName, amount);
    }
  }

  /**
   * 给予古真人属性奖励
   */
  private void grantGuzhenrenReward(Player player, double ghostHP, String ghostName, String field, String displayName) {
    // 根据不同属性使用不同的百分比范围
    double minPercent, maxPercent;
    switch (field) {
      case "zuida_hunpo" -> { // 魂魄上限 0.01% ~ 1%
        minPercent = 0.0001;
        maxPercent = 0.01;
      }
      case "zuida_zhenyuan" -> { // 真元上限 0.001% ~ 0.1%
        minPercent = 0.00001;
        maxPercent = 0.001;
      }
      case "zuida_jingli" -> { // 精力上限 0.01% ~ 0.1%
        minPercent = 0.0001;
        maxPercent = 0.001;
      }
      case "shouyuan", "qiyun", "renqi" -> { // 寿元/气运/人气 0.01% ~ 0.1%
        minPercent = 0.0001;
        maxPercent = 0.001;
      }
      default -> {
        minPercent = 0.0001;
        maxPercent = 0.001;
      }
    }

    double percentage = minPercent + this.random.nextDouble() * (maxPercent - minPercent);
    double amount = ghostHP * percentage;

    // 使用 GuzhenrenResourceBridge 调整属性
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isPresent()) {
      GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
      handle.adjustDouble(field, amount, false);
      // adjustDouble 内部会自动调用 syncEntity 同步到客户端

      player.sendSystemMessage(
          Component.literal(String.format("§6击杀了§r%s§6，获得了§e%s+%.2f", ghostName, displayName, amount)));
      ChestCavity.LOGGER.info(
          "[PlayerGhost] {} 击杀 {} 获得{} +{}",
          player.getName().getString(), ghostName, displayName, amount);
    }
  }

  /**
   * 奖励类型枚举
   *
   * <p>定义了击杀玩家幽灵可能获得的奖励类型及其掉落几率
   */
  private enum RewardType {
    MAX_HEALTH(50.0), // 最大生命值 50%
    ATTACK_DAMAGE(40.0), // 基础攻击力 40%
    MAX_HUNPO(3.0), // 魂魄上限 3%
    MAX_ZHENYUAN(3.0), // 真元上限 3%
    MAX_JINGLI(3.0), // 精力上限 3%
    SHOUYUAN(0.33), // 寿元 1/3%
    QIYUN(0.33), // 气运 1/3%
    RENQI(0.34); // 人气 1/3% (最后一个补齐到100%)

    private final double weight;

    RewardType(double weight) {
      this.weight = weight;
    }

    /**
     * 根据权重随机选择奖励类型
     */
    public static RewardType randomReward(net.minecraft.util.RandomSource random) {
      double totalWeight = 0.0;
      for (RewardType type : values()) {
        totalWeight += type.weight;
      }

      double roll = random.nextDouble() * totalWeight;
      double cumulative = 0.0;

      for (RewardType type : values()) {
        cumulative += type.weight;
        if (roll <= cumulative) {
          return type;
        }
      }

      // 兜底返回（理论上不会执行到）
      return MAX_HEALTH;
    }
  }
}
