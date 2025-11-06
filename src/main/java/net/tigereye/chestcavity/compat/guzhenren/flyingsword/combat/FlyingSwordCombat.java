package net.tigereye.chestcavity.compat.guzhenren.flyingsword.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContexts;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.fx.FlyingSwordFX;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.cooldown.FlyingSwordCooldownOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordCombatTuning;

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
   * <p>Phase 4: 使用 FlyingSwordCooldownOps 管理攻击冷却
   *
   * @param sword 飞剑实体
   */
  public static void tickCollisionAttack(FlyingSwordEntity sword) {
    if (!(sword.level() instanceof ServerLevel)) {
      return;
    }

    // Phase 4: 冷却递减（由 MultiCooldown 管理）
    int attackCooldown = FlyingSwordCooldownOps.tickDownAttackCooldown(sword);

    // 检查攻击冷却
    if (attackCooldown > 0) {
      return;
    }

    LivingEntity target = sword.getTargetEntity();

    // 调试：显示目标状态（默认静音，可在配置开启）
    if (FlyingSwordCombatTuning.COMBAT_DEBUG_LOGS && sword.tickCount % 20 == 0) {
      ChestCavity.LOGGER.debug(
          "[FlyingSword] Tick collision check: target={}, cooldown={}",
          target != null ? target.getName().getString() : "NULL",
          attackCooldown);
    }

    if (target == null || !target.isAlive()) {
      return;
    }

    // 检测碰撞
    double distance = sword.distanceTo(target);

    // 调试信息
    if (distance <= ATTACK_RANGE) {
      // 音效：挥砍
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
          .playSwing(sword);
      ChestCavity.LOGGER.debug(
          "[FlyingSword] Collision detected! Distance: {}, attempting attack...",
          String.format("%.2f", distance));

      boolean success = attackTarget(sword, target);

      if (!success) {
        ChestCavity.LOGGER.warn(
            "[FlyingSword] Attack returned false! Target: {}, Health: {}",
            target.getName().getString(),
            String.format("%.1f", target.getHealth()));
        return;
      }

      // Phase 4: 攻击成功，设置冷却到 MultiCooldown
      CalcContext ctx = CalcContexts.from(sword);
      int cd =
          FlyingSwordCalculator.calculateAttackCooldownTicks(
              ctx, FlyingSwordCombatTuning.ATTACK_COOLDOWN_TICKS);
      FlyingSwordCooldownOps.setAttackCooldown(sword, cd);
    }
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

    // 调试信息
    ChestCavity.LOGGER.debug(
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

    ChestCavity.LOGGER.debug(
        "[FlyingSword] Target status: invulnerableTime={}, health={}/{}, isInvulnerableTo={}",
        target.invulnerableTime,
        String.format("%.1f", target.getHealth()),
        String.format("%.1f", target.getMaxHealth()),
        target.isInvulnerableTo(damageSource));

    // 触发onHitEntity事件钩子
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
        .HitEntityContext hitCtx =
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .context.HitEntityContext(
            sword,
            (ServerLevel) sword.level(),
            owner,
            target,
            damageSource,
            speed,
            damage);
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
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

    ChestCavity.LOGGER.debug(
        "[FlyingSword] Attack result: success={}, targetHealthAfter={}",
        success,
        String.format("%.1f", target.getHealth()));

    if (success) {
      // 音效：命中
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
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
        // 精英判断：可在 Phase 8+ 或通过事件钩子扩展
        boolean isElite = false;
        // 经验倍率：可通过事件钩子修改 ExperienceGainContext.finalExpAmount
        int expGain =
            FlyingSwordCalculator.calculateExpGain(
                damage,
                isKill,
                isElite,
                1.0
                );

        // Phase 3: 触发经验获取事件（可修改或取消）
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
            .ExperienceGainContext.GainSource source =
            (target instanceof net.minecraft.world.entity.player.Player)
                ? net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                    .ExperienceGainContext.GainSource.KILL_PLAYER
                : (isKill
                    ? net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                        .ExperienceGainContext.GainSource.KILL_MOB
                    : net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
                        .ExperienceGainContext.GainSource.OTHER);
        var expCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
            .ExperienceGainContext(sword, Math.max(0, (int) Math.round(expGain)), source);
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .FlyingSwordEventRegistry.fireExperienceGain(expCtx);
        if (!expCtx.cancelled && expCtx.finalExpAmount > 0) {
          sword.addExperience(expCtx.finalExpAmount);
        }
        newLevel = sword.getSwordLevel();

        // 击杀提示
        if (isKill) {
          ChestCavity.LOGGER.debug(
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

      // Phase 3: 触发 PostHit 事件（伤害已造成，只读上下文）
      boolean wasKilled = !target.isAlive();
      var postHitCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
          .PostHitContext(sword, target, (float) damage, wasKilled);
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
          .FlyingSwordEventRegistry.firePostHit(postHitCtx);

      // Phase 3: 触发升级事件与特效
      if (newLevel > oldLevel) {
        var lvlCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context
            .LevelUpContext(sword, oldLevel, newLevel);
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
            .FlyingSwordEventRegistry.fireLevelUp(lvlCtx);
      }
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
