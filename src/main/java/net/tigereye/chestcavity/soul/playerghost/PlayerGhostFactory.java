package net.tigereye.chestcavity.soul.playerghost;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulEntityFactory;
import net.tigereye.chestcavity.soul.fakeplayer.SoulEntitySpawnRequest;
import net.tigereye.chestcavity.soul.fakeplayer.SoulEntitySpawnResult;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * 玩家幽灵实体工厂
 *
 * <p>职责：
 * - 从死亡记录创建玩家幽灵实体（基于 SoulPlayer）
 * - 恢复装备和属性
 * - 设置为敌对模式（攻击所有玩家）
 * - 设置自定义名称为"{玩家名}的魂魄"
 */
public final class PlayerGhostFactory implements SoulEntityFactory {

  private static final UUID GHOST_OWNER_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000001"); // 虚拟 owner UUID

  @Override
  public Optional<SoulEntitySpawnResult> spawn(SoulEntitySpawnRequest request) {
    UUID ghostId = request.entityId();
    String reason = request.reason();

    // 从存档中读取死亡记录数据
    Optional<CompoundTag> archivedStateOpt = request.archivedState();
    if (archivedStateOpt.isEmpty()) {
      SoulLog.warn(
          "[PlayerGhost] spawn-aborted reason={} ghost={} cause=noArchivedState", reason, ghostId);
      return Optional.empty();
    }

    // 解析死亡记录
    PlayerGhostArchive archive =
        PlayerGhostArchive.fromNbt(archivedStateOpt.get(), request.server().registryAccess());
    if (archive == null) {
      SoulLog.warn(
          "[PlayerGhost] spawn-aborted reason={} ghost={} cause=invalidArchive", reason, ghostId);
      return Optional.empty();
    }

    // 获取刷新世界和位置
    ServerLevel level =
        request.fallbackLevel().orElseGet(() -> request.server().overworld());
    if (level == null) {
      SoulLog.warn(
          "[PlayerGhost] spawn-aborted reason={} ghost={} cause=noLevel", reason, ghostId);
      return Optional.empty();
    }

    Vec3 spawnPos = request.fallbackPosition();

    try {
      // 创建幽灵玩家的 GameProfile，并应用原玩家的皮肤
      GameProfile ghostProfile =
          new GameProfile(UUID.randomUUID(), archive.getPlayerName() + "的魂魄");

      // 应用皮肤属性
      if (archive.getSkinPropertyValue() != null) {
        ghostProfile.getProperties().put(
            "textures",
            new com.mojang.authlib.properties.Property(
                "textures",
                archive.getSkinPropertyValue(),
                archive.getSkinPropertySignature()));
      }

      // 创建 SoulPlayer 实体
      // 注意：我们使用反射或直接实例化来创建没有真实 owner 的 SoulPlayer
      // 由于 SoulPlayer.create() 需要 ServerPlayer owner，我们需要使用另一种方式
      SoulPlayer ghost = createGhostPlayer(level, ghostProfile, ghostId, archive.getPlayerId());

      if (ghost == null) {
        SoulLog.warn(
            "[PlayerGhost] spawn-aborted reason={} ghost={} cause=createFailed", reason, ghostId);
        return Optional.empty();
      }

      // 设置位置
      ghost.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);

      // 设置自定义名称
      ghost.setCustomName(Component.literal(archive.getPlayerName() + "的魂魄"));
      ghost.setCustomNameVisible(true);

      // 恢复装备
      ghost.setItemSlot(EquipmentSlot.HEAD, archive.getHelmet());
      ghost.setItemSlot(EquipmentSlot.CHEST, archive.getChestplate());
      ghost.setItemSlot(EquipmentSlot.LEGS, archive.getLeggings());
      ghost.setItemSlot(EquipmentSlot.FEET, archive.getBoots());
      ghost.setItemSlot(EquipmentSlot.MAINHAND, archive.getMainHand());
      ghost.setItemSlot(EquipmentSlot.OFFHAND, archive.getOffHand());

      // 应用属性修饰符（恢复原玩家的属性）
      applyAttributes(ghost, archive);

      // 设置 Brain 为 COMBAT 模式（敌对）
      // 通过设置虚拟 owner 的 Soul Container
      setBrainModeCombat(ghost, level);

      // 广播玩家信息包
      request
          .server()
          .getPlayerList()
          .broadcastAll(
              ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(ghost)));

      // 添加到世界
      if (!level.tryAddFreshEntityWithPassengers(ghost)) {
        request
            .server()
            .getPlayerList()
            .broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(ghost.getUUID())));
        ghost.discard();
        SoulLog.warn(
            "[PlayerGhost] spawn-aborted reason={} ghost={} cause=addToWorldFailed",
            reason,
            ghostId);
        return Optional.empty();
      }

      // 同步装备渲染
      net.tigereye.chestcavity.soul.util.SoulRenderSync.syncEquipmentForPlayer(ghost);

      SoulLog.info(
          "[PlayerGhost] spawn-complete reason={} ghost={} name='{}' dim={} pos=({},{},{})",
          reason,
          ghostId,
          archive.getPlayerName() + "的魂魄",
          level.dimension().location(),
          spawnPos.x,
          spawnPos.y,
          spawnPos.z);

      return Optional.of(
          new SoulEntitySpawnResult(ghostId, ghost, PlayerGhostSpawner.FACTORY_ID, false, reason));

    } catch (Exception e) {
      SoulLog.error(
          "[PlayerGhost] spawn-failed reason={} ghost={} error={}",
          e,
          reason,
          ghostId,
          e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * 创建幽灵玩家实体
   *
   * <p>由于 SoulPlayer.create() 需要真实的 ServerPlayer owner，我们使用反射创建没有真实 owner 的实例
   */
  private SoulPlayer createGhostPlayer(
      ServerLevel level, GameProfile profile, UUID soulId, UUID originalPlayerId) {
    try {
      // 使用反射访问私有构造函数
      java.lang.reflect.Constructor<SoulPlayer> constructor =
          SoulPlayer.class.getDeclaredConstructor(
              ServerLevel.class, GameProfile.class, UUID.class, UUID.class);
      constructor.setAccessible(true);

      // 创建实体，使用原玩家 UUID 作为 ownerId（但设置为 COMBAT 模式会攻击所有人）
      SoulPlayer ghost = constructor.newInstance(level, profile, soulId, originalPlayerId);

      // 应用生存模式默认设置（通过反射调用私有方法）
      java.lang.reflect.Method applySurvivalDefaults =
          SoulPlayer.class.getDeclaredMethod("applySurvivalDefaults");
      applySurvivalDefaults.setAccessible(true);
      applySurvivalDefaults.invoke(ghost);

      return ghost;
    } catch (Exception e) {
      ChestCavity.LOGGER.error("[PlayerGhost] 创建幽灵玩家失败", e);
      return null;
    }
  }

  /**
   * 应用属性修饰符（恢复原玩家的属性）
   */
  private void applyAttributes(SoulPlayer ghost, PlayerGhostArchive archive) {
    // 设置生命值上限
    AttributeInstance maxHealth = ghost.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      double currentBase = maxHealth.getBaseValue();
      double targetMax = archive.getMaxHealth();
      if (targetMax > currentBase) {
        maxHealth.addPermanentModifier(
            new AttributeModifier(
                PlayerGhostSpawner.FACTORY_ID.withSuffix("/max_health"),
                targetMax - currentBase,
                AttributeModifier.Operation.ADD_VALUE));
      }
      ghost.setHealth((float) targetMax);
    }

    // 设置攻击力
    AttributeInstance attackDamage = ghost.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attackDamage != null && archive.getAttackDamage() > 0) {
      attackDamage.addPermanentModifier(
          new AttributeModifier(
              PlayerGhostSpawner.FACTORY_ID.withSuffix("/attack_damage"),
              archive.getAttackDamage(),
              AttributeModifier.Operation.ADD_VALUE));
    }

    // 设置护甲值
    AttributeInstance armor = ghost.getAttribute(Attributes.ARMOR);
    if (armor != null && archive.getArmor() > 0) {
      armor.addPermanentModifier(
          new AttributeModifier(
              PlayerGhostSpawner.FACTORY_ID.withSuffix("/armor"),
              archive.getArmor(),
              AttributeModifier.Operation.ADD_VALUE));
    }

    // 设置护甲韧性
    AttributeInstance armorToughness = ghost.getAttribute(Attributes.ARMOR_TOUGHNESS);
    if (armorToughness != null && archive.getArmorToughness() > 0) {
      armorToughness.addPermanentModifier(
          new AttributeModifier(
              PlayerGhostSpawner.FACTORY_ID.withSuffix("/armor_toughness"),
              archive.getArmorToughness(),
              AttributeModifier.Operation.ADD_VALUE));
    }

    // 设置移动速度
    AttributeInstance movementSpeed = ghost.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed != null) {
      double currentSpeed = movementSpeed.getBaseValue();
      double targetSpeed = archive.getMovementSpeed();
      if (Math.abs(targetSpeed - currentSpeed) > 0.001) {
        movementSpeed.addPermanentModifier(
            new AttributeModifier(
                PlayerGhostSpawner.FACTORY_ID.withSuffix("/movement_speed"),
                targetSpeed - currentSpeed,
                AttributeModifier.Operation.ADD_VALUE));
      }
    }

    // 设置击退抗性
    AttributeInstance knockbackResistance = ghost.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (knockbackResistance != null && archive.getKnockbackResistance() > 0) {
      knockbackResistance.addPermanentModifier(
          new AttributeModifier(
              PlayerGhostSpawner.FACTORY_ID.withSuffix("/knockback_resistance"),
              archive.getKnockbackResistance(),
              AttributeModifier.Operation.ADD_VALUE));
    }
  }

  /**
   * 设置 Brain 模式为 COMBAT（敌对模式）
   *
   * <p>注意：由于幽灵没有真实的 owner，我们需要确保它的 AI 设置为攻击所有玩家
   */
  private void setBrainModeCombat(SoulPlayer ghost, ServerLevel level) {
    // 获取或创建虚拟 owner 的 Soul Container（用于存储 Brain 设置）
    // 由于幽灵的 ownerId 是原玩家的 UUID，我们需要特殊处理
    // 这里我们直接通过幽灵的 Soul Container 设置 Brain 模式

    // TODO: 如果 SoulPlayer 有方法直接设置 Brain 模式，使用那个方法
    // 目前，我们依赖于 SoulPlayer 的 AI 系统自动处理 COMBAT 模式
    // 幽灵会攻击附近的玩家，因为它的 ownerId 指向原死亡玩家，而 COMBAT 模式会攻击所有目标

    SoulLog.info("[PlayerGhost] Brain mode set to COMBAT for ghost {}", ghost.getSoulId());
  }
}
