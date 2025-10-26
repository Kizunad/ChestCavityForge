package net.tigereye.chestcavity.soul.playerghost;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 玩家死亡记录存档
 *
 * <p>职责：
 * - 记录玩家死亡时的装备快照（盔甲槽 + 主副手）
 * - 记录玩家属性快照（生命值上限、攻击力、护甲值、移动速度等）
 * - 记录死亡位置和时间戳
 * - 提供 NBT 序列化/反序列化
 */
public final class PlayerGhostArchive {

  private final UUID playerId;
  private final String playerName;
  private final long deathTimestamp;
  private final ResourceKey<Level> dimension;
  private final double x;
  private final double y;
  private final double z;

  // Equipment slots
  private final ItemStack helmet;
  private final ItemStack chestplate;
  private final ItemStack leggings;
  private final ItemStack boots;
  private final ItemStack mainHand;
  private final ItemStack offHand;

  // Attributes
  private final double maxHealth;
  private final double attackDamage;
  private final double armor;
  private final double armorToughness;
  private final double movementSpeed;
  private final double knockbackResistance;

  // Skin information
  private final String skinPropertyValue;
  private final String skinPropertySignature;
  private final String skinTexture; // ResourceLocation as string
  private final String skinModel; // "default" or "slim"

  private PlayerGhostArchive(
      UUID playerId,
      String playerName,
      long deathTimestamp,
      ResourceKey<Level> dimension,
      double x,
      double y,
      double z,
      ItemStack helmet,
      ItemStack chestplate,
      ItemStack leggings,
      ItemStack boots,
      ItemStack mainHand,
      ItemStack offHand,
      double maxHealth,
      double attackDamage,
      double armor,
      double armorToughness,
      double movementSpeed,
      double knockbackResistance,
      String skinPropertyValue,
      String skinPropertySignature,
      String skinTexture,
      String skinModel) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.deathTimestamp = deathTimestamp;
    this.dimension = dimension;
    this.x = x;
    this.y = y;
    this.z = z;
    this.helmet = helmet;
    this.chestplate = chestplate;
    this.leggings = leggings;
    this.boots = boots;
    this.mainHand = mainHand;
    this.offHand = offHand;
    this.maxHealth = maxHealth;
    this.attackDamage = attackDamage;
    this.armor = armor;
    this.armorToughness = armorToughness;
    this.movementSpeed = movementSpeed;
    this.knockbackResistance = knockbackResistance;
    this.skinPropertyValue = skinPropertyValue;
    this.skinPropertySignature = skinPropertySignature;
    this.skinTexture = skinTexture;
    this.skinModel = skinModel;
  }

  /**
   * 从玩家捕获完整的死亡快照
   *
   * @param player 死亡的玩家
   * @return 玩家死亡记录
   */
  public static PlayerGhostArchive capture(ServerPlayer player) {
    // 捕获装备
    ItemStack helmet = player.getInventory().getArmor(3).copy();
    ItemStack chestplate = player.getInventory().getArmor(2).copy();
    ItemStack leggings = player.getInventory().getArmor(1).copy();
    ItemStack boots = player.getInventory().getArmor(0).copy();
    ItemStack mainHand = player.getMainHandItem().copy();
    ItemStack offHand = player.getOffhandItem().copy();

    // 捕获属性
    double maxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
    double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    double armor = player.getAttributeValue(Attributes.ARMOR);
    double armorToughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    double movementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
    double knockbackResistance = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);

    // 捕获皮肤信息
    net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil.SkinSnapshot skinSnapshot =
        net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil.capture(player);
    String skinPropertyValue = skinSnapshot.propertyValue();
    String skinPropertySignature = skinSnapshot.propertySignature();
    String skinTexture = skinSnapshot.texture() != null ? skinSnapshot.texture().toString() : null;
    String skinModel = skinSnapshot.model();

    return new PlayerGhostArchive(
        player.getUUID(),
        player.getGameProfile().getName(),
        player.level().getGameTime(),
        player.level().dimension(),
        player.getX(),
        player.getY(),
        player.getZ(),
        helmet,
        chestplate,
        leggings,
        boots,
        mainHand,
        offHand,
        maxHealth,
        attackDamage,
        armor,
        armorToughness,
        movementSpeed,
        knockbackResistance,
        skinPropertyValue,
        skinPropertySignature,
        skinTexture,
        skinModel);
  }

  /**
   * 序列化到 NBT
   *
   * @param provider 注册中心提供者
   * @return NBT 标签
   */
  public CompoundTag toNbt(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();

    // 基本信息
    tag.putUUID("player_id", playerId);
    tag.putString("player_name", playerName);
    tag.putLong("death_timestamp", deathTimestamp);
    tag.putString("dimension", dimension.location().toString());
    tag.putDouble("x", x);
    tag.putDouble("y", y);
    tag.putDouble("z", z);

    // 装备（只保存非空的 ItemStack，空的用特殊标记）
    CompoundTag equipmentTag = new CompoundTag();
    if (!helmet.isEmpty()) {
      equipmentTag.put("helmet", helmet.save(provider));
    }
    if (!chestplate.isEmpty()) {
      equipmentTag.put("chestplate", chestplate.save(provider));
    }
    if (!leggings.isEmpty()) {
      equipmentTag.put("leggings", leggings.save(provider));
    }
    if (!boots.isEmpty()) {
      equipmentTag.put("boots", boots.save(provider));
    }
    if (!mainHand.isEmpty()) {
      equipmentTag.put("mainHand", mainHand.save(provider));
    }
    if (!offHand.isEmpty()) {
      equipmentTag.put("offHand", offHand.save(provider));
    }
    tag.put("equipment", equipmentTag);

    // 属性
    CompoundTag attributes = new CompoundTag();
    attributes.putDouble("max_health", maxHealth);
    attributes.putDouble("attack_damage", attackDamage);
    attributes.putDouble("armor", armor);
    attributes.putDouble("armor_toughness", armorToughness);
    attributes.putDouble("movement_speed", movementSpeed);
    attributes.putDouble("knockback_resistance", knockbackResistance);
    tag.put("attributes", attributes);

    // 皮肤
    if (skinPropertyValue != null) {
      tag.putString("skin_property_value", skinPropertyValue);
    }
    if (skinPropertySignature != null) {
      tag.putString("skin_property_signature", skinPropertySignature);
    }
    if (skinTexture != null) {
      tag.putString("skin_texture", skinTexture);
    }
    if (skinModel != null) {
      tag.putString("skin_model", skinModel);
    }

    return tag;
  }

  /**
   * 从 NBT 反序列化
   *
   * @param tag NBT 标签
   * @param provider 注册中心提供者
   * @return 玩家死亡记录，如果解析失败则返回 null
   */
  public static PlayerGhostArchive fromNbt(CompoundTag tag, HolderLookup.Provider provider) {
    try {
      // 基本信息
      UUID playerId = tag.getUUID("player_id");
      String playerName = tag.getString("player_name");
      long deathTimestamp = tag.getLong("death_timestamp");
      ResourceLocation dimLoc = ResourceLocation.parse(tag.getString("dimension"));
      ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimLoc);
      double x = tag.getDouble("x");
      double y = tag.getDouble("y");
      double z = tag.getDouble("z");

      // 装备（从新的格式读取，兼容旧格式）
      CompoundTag equipmentTag = tag.getCompound("equipment");
      ItemStack helmet = ItemStack.EMPTY;
      ItemStack chestplate = ItemStack.EMPTY;
      ItemStack leggings = ItemStack.EMPTY;
      ItemStack boots = ItemStack.EMPTY;
      ItemStack mainHand = ItemStack.EMPTY;
      ItemStack offHand = ItemStack.EMPTY;

      // 尝试新格式（CompoundTag）
      if (equipmentTag.contains("helmet")) {
        helmet = ItemStack.parse(provider, equipmentTag.getCompound("helmet")).orElse(ItemStack.EMPTY);
      }
      if (equipmentTag.contains("chestplate")) {
        chestplate = ItemStack.parse(provider, equipmentTag.getCompound("chestplate")).orElse(ItemStack.EMPTY);
      }
      if (equipmentTag.contains("leggings")) {
        leggings = ItemStack.parse(provider, equipmentTag.getCompound("leggings")).orElse(ItemStack.EMPTY);
      }
      if (equipmentTag.contains("boots")) {
        boots = ItemStack.parse(provider, equipmentTag.getCompound("boots")).orElse(ItemStack.EMPTY);
      }
      if (equipmentTag.contains("mainHand")) {
        mainHand = ItemStack.parse(provider, equipmentTag.getCompound("mainHand")).orElse(ItemStack.EMPTY);
      }
      if (equipmentTag.contains("offHand")) {
        offHand = ItemStack.parse(provider, equipmentTag.getCompound("offHand")).orElse(ItemStack.EMPTY);
      }

      // 属性
      CompoundTag attributes = tag.getCompound("attributes");
      double maxHealth = attributes.getDouble("max_health");
      double attackDamage = attributes.getDouble("attack_damage");
      double armor = attributes.getDouble("armor");
      double armorToughness = attributes.getDouble("armor_toughness");
      double movementSpeed = attributes.getDouble("movement_speed");
      double knockbackResistance = attributes.getDouble("knockback_resistance");

      // 皮肤
      String skinPropertyValue = tag.contains("skin_property_value") ? tag.getString("skin_property_value") : null;
      String skinPropertySignature = tag.contains("skin_property_signature") ? tag.getString("skin_property_signature") : null;
      String skinTexture = tag.contains("skin_texture") ? tag.getString("skin_texture") : null;
      String skinModel = tag.contains("skin_model") ? tag.getString("skin_model") : null;

      return new PlayerGhostArchive(
          playerId,
          playerName,
          deathTimestamp,
          dimension,
          x,
          y,
          z,
          helmet,
          chestplate,
          leggings,
          boots,
          mainHand,
          offHand,
          maxHealth,
          attackDamage,
          armor,
          armorToughness,
          movementSpeed,
          knockbackResistance,
          skinPropertyValue,
          skinPropertySignature,
          skinTexture,
          skinModel);
    } catch (Exception e) {
      return null;
    }
  }

  // Getters
  public UUID getPlayerId() {
    return playerId;
  }

  public String getPlayerName() {
    return playerName;
  }

  public long getDeathTimestamp() {
    return deathTimestamp;
  }

  public ResourceKey<Level> getDimension() {
    return dimension;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public ItemStack getHelmet() {
    return helmet.copy();
  }

  public ItemStack getChestplate() {
    return chestplate.copy();
  }

  public ItemStack getLeggings() {
    return leggings.copy();
  }

  public ItemStack getBoots() {
    return boots.copy();
  }

  public ItemStack getMainHand() {
    return mainHand.copy();
  }

  public ItemStack getOffHand() {
    return offHand.copy();
  }

  public double getMaxHealth() {
    return maxHealth;
  }

  public double getAttackDamage() {
    return attackDamage;
  }

  public double getArmor() {
    return armor;
  }

  public double getArmorToughness() {
    return armorToughness;
  }

  public double getMovementSpeed() {
    return movementSpeed;
  }

  public double getKnockbackResistance() {
    return knockbackResistance;
  }

  public String getSkinPropertyValue() {
    return skinPropertyValue;
  }

  public String getSkinPropertySignature() {
    return skinPropertySignature;
  }

  public String getSkinTexture() {
    return skinTexture;
  }

  public String getSkinModel() {
    return skinModel;
  }
}
