package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 魂道分身的生命周期 Hook 接口。
 *
 * <p>第三方逻辑通过 {@link HunDaoSoulAvatarHookRegistry} 注册，实现 onSpawn/onKill/onDeath 等监听，
 * 便于扩展而不侵入实体本体。</p>
 */
public interface HunDaoSoulAvatarHook {

  /** 首次加入世界（仅服务端）时触发。 */
  default void onSpawn(HunDaoSoulAvatarEntity avatar) {}

  /** 每 tick 回调（仅服务端）。用于行为或状态驱动。 */
  default void onServerTick(HunDaoSoulAvatarEntity avatar) {}

  /** 在分身击杀任意其他生物后触发。 */
  default void onKillEntity(HunDaoSoulAvatarEntity avatar, LivingEntity victim) {}

  /** 在分身死亡时触发。 */
  default void onDeath(HunDaoSoulAvatarEntity avatar, DamageSource source) {}

  /** 当实体以任意原因被移除时回调。 */
  default void onRemoved(HunDaoSoulAvatarEntity avatar, Entity.RemovalReason reason) {}

  /** 同步魂魄到血量时触发。 */
  default void onSyncHealth(HunDaoSoulAvatarEntity avatar) {}

  /** 计算伤害减免时触发。 */
  default double modifyDamage(HunDaoSoulAvatarEntity avatar, double damage) {
    return damage;
  }
}
