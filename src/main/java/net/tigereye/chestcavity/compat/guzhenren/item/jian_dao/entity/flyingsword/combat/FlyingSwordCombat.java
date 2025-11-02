package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContexts;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx.FlyingSwordFX;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordCombatTuning;

/**
 * 飞剑战斗系统
 *
 * <p>处理飞剑的攻击逻辑：
 * <ul>
 *   <li>碰撞检测</li>
 *   <li>速度²伤害计算</li>
 *   <li>攻击冷却管理</li>
 *   <li>经验获取</li>
 *   <li>耐久消耗</li>
 * </ul>
 */
public final class FlyingSwordCombat {

  /** 攻击范围（迁移到 Tuning，保留常量只作本地引用） */
  public static final double ATTACK_RANGE = FlyingSwordCombatTuning.ATTACK_RANGE;

  private FlyingSwordCombat() {}

  /**
   * 碰撞攻击检测
   *
   * @param sword 飞剑实体
   * @param attackCooldown 当前攻击冷却
   * @return 新的攻击冷却值
   */
  public static int tickCollisionAttack(FlyingSwordEntity sword, int attackCooldown) {
    if (!(sword.level() instanceof ServerLevel)) {
      return attackCooldown;
    }

    // 检查攻击冷却
    if (attackCooldown > 0) {
      return attackCooldown - 1;
    }

    LivingEntity target = sword.getTargetEntity();

    // 调试：显示目标状态（默认静音，可在配置开启）
    if (FlyingSwordCombatTuning.COMBAT_DEBUG_LOGS && sword.tickCount % 20 == 0) {
      ChestCavity.LOGGER.info(
          "[FlyingSword] Tick collision check: target={}, cooldown={}",
          target != null ? target.getName().getString() : "NULL",
          attackCooldown);
    }

    if (target == null || !target.isAlive()) {
      return 0;
    }

    // 检测碰撞
    double distance = sword.distanceTo(target);

    // 调试信息
    if (distance <= ATTACK_RANGE) {
      // 音效：挥砍
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops.SoundOps
          .playSwing(sword);
      ChestCavity.LOGGER.info(
          "[FlyingSword] Collision detected! Distance: {}, attempting attack...",
          String.format("%.2f", distance));

      boolean success = attackTarget(sword, target);

      if (!success) {
        ChestCavity.LOGGER.warn(
            "[FlyingSword] Attack returned false! Target: {}, Health: {}",
            target.getName().getString(),
            String.format("%.1f", target.getHealth()));
        return 0;
      }

      // 攻击成功，设置冷却（可被上下文钩子/经验等影响）
      CalcContext ctx = CalcContexts.from(sword);
      int cd =
          FlyingSwordCalculator.calculateAttackCooldownTicks(
              ctx, FlyingSwordCombatTuning.ATTACK_COOLDOWN_TICKS);
      return cd;
    }

    return 0;
  }

  /**
   * 对目标造成速度²伤害
   *
   * @param sword 飞剑实体
   * @param target 攻击目标
   * @return 是否成功造成伤害
   */
  public static boolean attackTarget(FlyingSwordEntity sword, LivingEntity target) {
    if (sword.level().isClientSide || target == null || !target.isAlive()) {
      return false;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return false;
    }

    // 计算伤害
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();
    int level = sword.getSwordLevel();
    double levelScale =
        FlyingSwordCalculator.calculateLevelScale(level, FlyingSwordTuning.DAMAGE_PER_LEVEL);

    CalcContext ctx = CalcContexts.from(sword);
    double damage =
        FlyingSwordCalculator.calculateDamageWithContext(
            sword.getSwordAttributes().damageBase,
            speed,
            FlyingSwordTuning.V_REF,
            sword.getSwordAttributes().velDmgCoef,
            levelScale,
            ctx);

    // 调试信息（总是输出到日志）
    ChestCavity.LOGGER.info(
        "[FlyingSword] Attack START: target={}, speed={}, damage={}, baseDmg={}, vRef={}, velCoef={}, levelScale={}",
        target.getName().getString(),
        String.format("%.3f", speed),
        String.format("%.2f", damage),
        sword.getSwordAttributes().damageBase,
        FlyingSwordTuning.V_REF,
        sword.getSwordAttributes().velDmgCoef,
        String.format("%.2f", levelScale));

    // 检查目标状态
    // 创建伤害源（如果owner是Player则使用playerAttack，否则使用mobAttack）
    DamageSource damageSource;
    if (owner instanceof Player player) {
      damageSource = sword.damageSources().playerAttack(player);
    } else {
      damageSource = sword.damageSources().mobAttack(owner);
    }

    ChestCavity.LOGGER.info(
        "[FlyingSword] Target status: invulnerableTime={}, health={}/{}, isInvulnerableTo={}",
        target.invulnerableTime,
        String.format("%.1f", target.getHealth()),
        String.format("%.1f", target.getMaxHealth()),
        target.isInvulnerableTo(damageSource));

    // 触发onHitEntity事件钩子
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
        .HitEntityContext hitCtx =
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
            .context.HitEntityContext(
            sword,
            (ServerLevel) sword.level(),
            owner,
            target,
            damageSource,
            speed,
            damage);
    net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
        .FlyingSwordEventRegistry.fireHitEntity(hitCtx);

    // 检查是否被钩子取消
    if (hitCtx.cancelled) {
      ChestCavity.LOGGER.debug("[FlyingSword] Attack cancelled by event hook");
      return false;
    }

    // 使用钩子修改后的伤害
    damage = hitCtx.damage;

    // 造成伤害
    boolean success = target.hurt(damageSource, (float) damage);

    ChestCavity.LOGGER.info(
        "[FlyingSword] Attack result: success={}, targetHealthAfter={}",
        success,
        String.format("%.1f", target.getHealth()));

    if (success) {
      // 音效：命中
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops.SoundOps
          .playHit(sword);
      // 调试信息
      ChestCavity.LOGGER.debug(
          "[FlyingSword] Hit success! Target health: {}/{}",
          String.format("%.1f", target.getHealth()),
          String.format("%.1f", target.getMaxHealth()));

      // 攻击碰撞粒子特效
      if (sword.level() instanceof ServerLevel serverLevel) {
        FlyingSwordFX.spawnAttackImpact(
            serverLevel, sword, target.position().add(0, target.getBbHeight() / 2, 0), damage);
      }

      // 耐久损耗（可被钩子跳过）
      if (!hitCtx.skipDurability) {
        float duraLoss =
            FlyingSwordCalculator.calculateDurabilityLossWithContext(
                (float) damage, sword.getSwordAttributes().duraLossRatio, false, ctx);
        sword.damageDurability(duraLoss);
      }

      // 经验获取（可被钩子跳过）
      int oldLevel = sword.getSwordLevel();
      int newLevel = oldLevel;
      if (!hitCtx.skipExp) {
        boolean isKill = !target.isAlive();
        boolean isElite = false; // TODO: 判断精英怪
        int expGain =
            FlyingSwordCalculator.calculateExpGain(
                damage,
                isKill,
                isElite,
                1.0 // TODO: 经验倍率
                );

        sword.addExperience(expGain);
        newLevel = sword.getSwordLevel();

        // 击杀提示
        if (isKill) {
          ChestCavity.LOGGER.info(
              "[FlyingSword] Killed {}! Gained {} exp", target.getName().getString(), expGain);
        }
      }

      // 命中后追加真伤（若配置 > 0）
      double extraTrue = sword.getSwordAttributes().trueDamagePerHit;
      if (extraTrue > 0.0) {
        try {
          target.hurt(sword.damageSources().magic(), (float) extraTrue);
        } catch (Throwable ignored) {}
      }

      // 升级特效
      if (newLevel > oldLevel && sword.level() instanceof ServerLevel serverLevel) {
        FlyingSwordFX.spawnLevelUpEffect(serverLevel, sword, newLevel);
      }
    } else {
      // 调试信息
      ChestCavity.LOGGER.debug(
          "[FlyingSword] Hit failed! Target may be invulnerable or damage was 0");
    }

    return success;
  }

  /**
   * 计算伤害（不执行攻击）
   *
   * <p>用于UI显示和调试
   *
   * @param sword 飞剑实体
   * @return 计算的伤害值
   */
  public static double calculateCurrentDamage(FlyingSwordEntity sword) {
    Vec3 velocity = sword.getDeltaMovement();
    double speed = velocity.length();
    int level = sword.getSwordLevel();
    double levelScale =
        FlyingSwordCalculator.calculateLevelScale(level, FlyingSwordTuning.DAMAGE_PER_LEVEL);
    CalcContext ctx = CalcContexts.from(sword);
    return FlyingSwordCalculator.calculateDamageWithContext(
        sword.getSwordAttributes().damageBase,
        speed,
        FlyingSwordTuning.V_REF,
        sword.getSwordAttributes().velDmgCoef,
        levelScale,
        ctx);
  }
}
