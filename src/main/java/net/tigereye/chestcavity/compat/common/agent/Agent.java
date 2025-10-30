package net.tigereye.chestcavity.compat.common.agent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * 统一“行动体”抽象：面向玩家与非玩家生物（NPC/怪物）的一致读取/操作入口。
 *
 * <p>用法：
 * - 通过 {@link Agents#from(Entity)} 或 {@link Agents#of(LivingEntity)} 获取实例。
 * - 读取：{@link #living()}、{@link #asPlayer()}、{@link #chestCavity()}、{@link #serverTime()}。
 * - 资源：配合 {@link Agents#openResource(LivingEntity)} 与 {@link Agents} 中的静态 Ops 使用。
 */
public final class Agent {

  private final LivingEntity living;

  Agent(LivingEntity living) {
    this.living = Objects.requireNonNull(living, "living");
  }

  /** 底层实体（玩家或非玩家）。 */
  public LivingEntity living() {
    return living;
  }

  /** 若是玩家则返回 {@link ServerPlayer}。 */
  public Optional<ServerPlayer> asPlayer() {
    return living instanceof ServerPlayer sp ? Optional.of(sp) : Optional.empty();
  }

  /** 唯一标识（同实体 UUID）。 */
  public UUID id() {
    return living.getUUID();
  }

  /** 统一访问胸腔实例（玩家/NPC 一致）。 */
  public ChestCavityInstance chestCavity() {
    return ChestCavityEntity.of(living).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
  }

  /** 当前服务器时间（tick）。若不在服务端，返回 0。 */
  public long serverTime() {
    return living.level() instanceof ServerLevel sl ? sl.getServer().getTickCount() : 0L;
  }

  /** 是否处于服务端世界。 */
  public boolean isServerLevel() {
    return living.level() instanceof ServerLevel;
  }

  /** 快捷：打开 Guzhenren 资源句柄（如真元/精力/魂魄）。 */
  public Optional<ResourceHandle> openResource() {
    return Agents.openResource(living);
  }

  /** 快捷：获取属性实例（可能为 null）。 */
  public AttributeInstance getAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr) {
    return living.getAttribute(attr);
  }
}

