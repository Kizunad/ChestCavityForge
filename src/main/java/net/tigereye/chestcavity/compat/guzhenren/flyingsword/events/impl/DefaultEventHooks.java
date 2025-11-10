package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.*;

/**
 * 默认事件钩子实现
 *
 * <p>包含核心逻辑：
 *
 * <ul>
 *   <li>受击折返/虚弱
 *   <li>消散时回写物品NBT
 *   <li>其他示例钩子
 * </ul>
 *
 * <p>这个钩子会在模组初始化时自动注册。
 */
public class DefaultEventHooks implements FlyingSwordEventHook {

  /** 虚弱状态标识（用于实体NBT临时标记） */
  private static final String WEAKENED_KEY = "cc_flying_sword_weakened";

  /** 虚弱剩余时间 */
  private static final String WEAKENED_TIME_KEY = "cc_flying_sword_weakened_time";

  @Override
  public void onSpawn(SpawnContext ctx) {
    // 示例：生成时记录日志
    ChestCavity.LOGGER.debug(
        "[FlyingSword] Spawned sword for {} at {}", ctx.owner.getName().getString(), ctx.spawnPos);
  }

  @Override
  public void onTick(TickContext ctx) {
    // 处理虚弱状态倒计时
    FlyingSwordEntity sword = ctx.sword;
    CompoundTag nbt = new CompoundTag();
    sword.saveWithoutId(nbt);

    if (nbt.getBoolean(WEAKENED_KEY)) {
      int remaining = nbt.getInt(WEAKENED_TIME_KEY);
      remaining--;

      if (remaining <= 0) {
        // 虚弱状态结束
        nbt.remove(WEAKENED_KEY);
        nbt.remove(WEAKENED_TIME_KEY);
        ChestCavity.LOGGER.debug("[FlyingSword] Weakened state ended for sword {}", sword.getId());
      } else {
        nbt.putInt(WEAKENED_TIME_KEY, remaining);
      }

      sword.load(nbt);

      // 虚弱期间：跳过破块，降低速度上限
      if (nbt.getBoolean(WEAKENED_KEY)) {
        ctx.skipBlockBreak = true;
        // 速度限制通过修改速度实现
        Vec3 vel = sword.getDeltaMovement();
        double weakenedSpeedCap = sword.getSwordAttributes().speedMax * 0.4; // 40%速度
        if (vel.length() > weakenedSpeedCap) {
          sword.setDeltaMovement(vel.normalize().scale(weakenedSpeedCap));
        }
      }
    }
  }

  @Override
  public void onHurt(HurtContext ctx) {
    // 默认：受到伤害时有概率触发折返
    if (ctx.damage > 5.0f) {
      // 高伤害→必定折返
      ctx.triggerRetreat = true;
    } else if (ctx.damage > 2.0f && ctx.level.random.nextFloat() < 0.5f) {
      // 中等伤害→50%概率折返
      ctx.triggerRetreat = true;
    }

    // 受击后进入虚弱状态
    if (ctx.damage > 0) {
      ctx.triggerWeakened = true;
      ctx.weakenedDuration = (int) (60 + ctx.damage * 10); // 基础3秒 + 每点伤害0.5秒
    }

    ChestCavity.LOGGER.debug(
        "[FlyingSword] Hurt: damage={}, retreat={}, weakened={}",
        ctx.damage,
        ctx.triggerRetreat,
        ctx.triggerWeakened);
  }

  @Override
  public void onDespawnOrRecall(DespawnContext ctx) {
    // 回写物品NBT（保存飞剑状态到物品）
    if (ctx.targetStack != null && !ctx.targetStack.isEmpty()) {
      CompoundTag swordData = new CompoundTag();

      // 保存核心状态
      swordData.putInt("SwordLevel", ctx.sword.getSwordLevel());
      swordData.putInt("Experience", ctx.sword.getExperience());
      swordData.putFloat("Durability", ctx.sword.getDurability());

      // 保存属性
      CompoundTag attrs = new CompoundTag();
      ctx.sword.getSwordAttributes().saveToNBT(attrs);
      swordData.put("Attributes", attrs);

      // 合并自定义数据
      if (!ctx.customData.isEmpty()) {
        swordData.put("CustomData", ctx.customData);
      }

      // 写入物品CustomData
      CompoundTag rootTag =
          ctx.targetStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      rootTag.put("cc_flying_sword_state", swordData);
      ctx.targetStack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));

      ChestCavity.LOGGER.info(
          "[FlyingSword] Saved sword state to item: level={}, exp={}, durability={}",
          ctx.sword.getSwordLevel(),
          ctx.sword.getExperience(),
          ctx.sword.getDurability());
    }

    // 消散特效
    if (ctx.reason == DespawnContext.Reason.RECALLED
        || ctx.reason == DespawnContext.Reason.CAPTURED) {
      FlyingSwordFX.spawnRecallEffect(ctx.level, ctx.sword);
    }

    ChestCavity.LOGGER.debug(
        "[FlyingSword] Despawning sword: reason={}, prevented={}", ctx.reason, ctx.preventDespawn);
  }

  @Override
  public void onInteract(InteractContext ctx) {
    // 示例：非主人交互时显示信息
    if (!ctx.isOwner) {
      ctx.interactor.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              String.format(
                  "[飞剑] 这把飞剑属于 %s",
                  ctx.sword.getOwner() != null
                      ? ctx.sword.getOwner().getName().getString()
                      : "未知")));
    }
  }

  // ========== 辅助方法 ==========

  /**
   * 应用折返效果（反弹向主人）
   *
   * @param sword 飞剑
   */
  public static void applyRetreat(FlyingSwordEntity sword) {
    var owner = sword.getOwner();
    if (owner == null) return;

    Vec3 toOwner = owner.getEyePosition().subtract(sword.position()).normalize();
    Vec3 retreatVelocity = toOwner.scale(sword.getSwordAttributes().speedMax * 0.6);
    sword.setDeltaMovement(retreatVelocity);

    // 粒子特效
    if (sword.level() instanceof net.minecraft.server.level.ServerLevel level) {
      level.sendParticles(
          net.minecraft.core.particles.ParticleTypes.SMOKE,
          sword.getX(),
          sword.getY(),
          sword.getZ(),
          10,
          0.2,
          0.2,
          0.2,
          0.05);
    }

    ChestCavity.LOGGER.debug("[FlyingSword] Applied retreat to sword {}", sword.getId());
  }

  /**
   * 应用虚弱状态
   *
   * @param sword 飞剑
   * @param duration 持续时间（ticks）
   */
  public static void applyWeakened(FlyingSwordEntity sword, int duration) {
    CompoundTag nbt = new CompoundTag();
    sword.saveWithoutId(nbt);
    nbt.putBoolean(WEAKENED_KEY, true);
    nbt.putInt(WEAKENED_TIME_KEY, duration);
    sword.load(nbt);

    ChestCavity.LOGGER.debug(
        "[FlyingSword] Applied weakened state to sword {} for {} ticks", sword.getId(), duration);
  }

  /** 检查飞剑是否处于虚弱状态 */
  public static boolean isWeakened(FlyingSwordEntity sword) {
    CompoundTag nbt = new CompoundTag();
    sword.saveWithoutId(nbt);
    return nbt.getBoolean(WEAKENED_KEY);
  }
}
