package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.FlyingSwordCalculator;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContexts;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordBlockBreakTuning;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordCoreTuning;
import net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps;

/**
 * 破块逻辑（服务端）。
 */
public final class BlockBreakOps {
  private BlockBreakOps() {}

  public static void tickBlockBreak(FlyingSwordEntity sword) {
    if (!(sword.level() instanceof ServerLevel level)) {
      return;
    }
    if (!FlyingSwordCoreTuning.ENABLE_BLOCK_BREAK) {
      if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            String.format(
                "[FlyingSword] BlockBreak disabled by config (id=%d)", sword.getId()));
      }
      return;
    }

    // 玩家偏好：若有主人且关闭了破块，则直接跳过
    var owner = sword.getOwner();
    boolean allowByOwner =
        owner instanceof net.minecraft.world.entity.player.Player player
            ? PlayerPreferenceOps.resolve(
                player,
                PlayerPreferenceOps.SWORD_SLASH_BLOCK_BREAK,
                PlayerPreferenceOps::defaultSwordSlashBlockBreak)
            : PlayerPreferenceOps.defaultSwordSlashBlockBreak();
    if (!allowByOwner) {
      if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            String.format(
                "[FlyingSword] BlockBreak disabled by player preference (id=%d)", sword.getId()));
      }
      return;
    }

    // 速度与阈值
    var ctx = CalcContexts.from(sword);
    double effMax =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedMax(sword.getSwordAttributes().speedMax, ctx);
    double speed = sword.getDeltaMovement().length();

    // 固定速度阈值优先生效
    double absThr = Math.max(0.0, FlyingSwordBlockBreakTuning.BREAK_MIN_SPEED_ABS);
    if (absThr > 0.0 && speed < absThr) {
      if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            String.format(
                "[FlyingSword] BlockBreak below ABS threshold: speed=%.3f thr=%.3f (id=%d)",
                speed, absThr, sword.getId()));
      }
      return;
    }

    if (effMax <= 0.0) {
      if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            String.format(
                "[FlyingSword] BlockBreak skip: effMax<=0 (id=%d)", sword.getId()));
      }
      return;
    }
    double speedPercent = speed / effMax;
    // 兼容保留百分比阈值（当 ABS 为 0 时生效）
    if (absThr == 0.0
        && speedPercent < FlyingSwordBlockBreakTuning.BREAK_MIN_SPEED_PERCENT) {
      if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            String.format(
                "[FlyingSword] BlockBreak below PERCENT threshold: speed=%.3f effMax=%.3f perc=%.2f%% thr=%.2f%% (id=%d)",
                speed,
                effMax,
                speedPercent * 100.0,
                FlyingSwordBlockBreakTuning.BREAK_MIN_SPEED_PERCENT * 100.0,
                sword.getId()));
      }
      return;
    }

    // 生效镐等级（随速度阶段加成）
    int extraTier =
        (int) Math.floor(speedPercent * FlyingSwordBlockBreakTuning.BREAK_EXTRA_TIER_AT_MAX);
    int toolTier = sword.getSwordAttributes().toolTier + Math.max(0, extraTier);

    // 命中盒周围扫描
    double r = FlyingSwordBlockBreakTuning.BREAK_SCAN_RADIUS;
    AABB scan = sword.getBoundingBox().inflate(r);
    int minX = Mth.floor(scan.minX);
    int minY = Mth.floor(scan.minY);
    int minZ = Mth.floor(scan.minZ);
    int maxX = Mth.floor(scan.maxX);
    int maxY = Mth.floor(scan.maxY);
    int maxZ = Mth.floor(scan.maxZ);

    int tested = 0;
    int solid = 0;
    int mineableCount = 0;
    int eligible = 0;
    int broken = 0;
    boolean playedSound = false;
    for (int x = minX; x <= maxX && broken < FlyingSwordBlockBreakTuning.BREAK_MAX_BLOCKS_PER_TICK; x++) {
      for (int y = minY; y <= maxY && broken < FlyingSwordBlockBreakTuning.BREAK_MAX_BLOCKS_PER_TICK; y++) {
        for (int z = minZ; z <= maxZ && broken < FlyingSwordBlockBreakTuning.BREAK_MAX_BLOCKS_PER_TICK; z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (!level.isLoaded(pos)) continue;

          BlockState state = level.getBlockState(pos);
          tested++;
          if (state.isAir()) continue;
          solid++;
          if (!isMineable(state)) continue;
          mineableCount++;
          if (!canMine(state, toolTier)) continue;
          eligible++;

          // 排除不可破坏
          if (isUnbreakable(level, pos, state)) continue;

          // Phase 3: 触发 BlockBreakAttempt 事件（破块之前）
          boolean canBreak = canMine(state, toolTier);
          var attemptCtx = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
              .context.BlockBreakAttemptContext(sword, pos, state, canBreak);
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
              .FlyingSwordEventRegistry.fireBlockBreakAttempt(attemptCtx);

          // Phase 3: 检查是否被事件取消
          if (attemptCtx.cancelled || !attemptCtx.canBreak) {
            continue; // 跳过该方块
          }

          // 破坏
          if (level.destroyBlock(pos, true, sword)) {
            broken++;
            if (!playedSound) {
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.SoundOps
                  .playBlockBreak(sword);
              playedSound = true;
            }
            if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
              net.tigereye.chestcavity.ChestCavity.LOGGER.info(
                  String.format(
                      "[FlyingSword] BlockBreak destroyed %s at %s (id=%d)",
                      state.toString(),
                      pos.toShortString(),
                      sword.getId()));
            }
            float hardness = state.getDestroySpeed(level, pos);
            float duraBlock =
                FlyingSwordCalculator.calculateDurabilityLossFromBlock(hardness, toolTier);
            float total =
                net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
                    .FlyingSwordCalculator.calculateDurabilityLossWithContext(
                        duraBlock, sword.getSwordAttributes().duraLossRatio, true, ctx);

            double decel = FlyingSwordBlockBreakTuning.BREAK_DECEL_PER_BLOCK;

            // 触发onBlockBreak事件钩子
            var blockOwner = sword.getOwner();
            if (blockOwner != null) {
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
                  .context.BlockBreakContext breakCtx =
                  new net.tigereye.chestcavity.compat.guzhenren.flyingsword
                      .events.context.BlockBreakContext(
                      sword,
                      level,
                      blockOwner,
                      pos,
                      state,
                      hardness,
                      speed,
                      toolTier,
                      total,
                      decel);
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.events
                  .FlyingSwordEventRegistry.fireBlockBreak(breakCtx);

              // 使用钩子修改后的值
              total = (float) breakCtx.durabilityLoss;
              decel = breakCtx.deceleration;

              // 应用耐久损耗（可被钩子跳过）
              if (!breakCtx.skipDurability) {
                sword.damageDurability(total);
              }
            } else {
              // 无主人时直接应用
              sword.damageDurability(total);
            }

            // 应用减速
            if (decel > 0.0) {
              Vec3 v = sword.getDeltaMovement().scale(Math.max(0.0, 1.0 - decel));
              sword.setDeltaMovement(v);
            }
          }
        }
      }
    }

    if (FlyingSwordBlockBreakTuning.BREAK_DEBUG_LOGS && sword.tickCount % 20 == 0) {
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          String.format(
              "[FlyingSword] BlockBreak summary id=%d speed=%.3f absThr=%.3f effMax=%.3f perc=%.2f%% thr%%=%.2f%% scanR=%.1f max/tick=%d toolBase=%d extra=%d effTool=%d tested=%d solid=%d mineable=%d eligible=%d broken=%d",
              sword.getId(),
              speed,
              absThr,
              effMax,
              speedPercent * 100.0,
              FlyingSwordBlockBreakTuning.BREAK_MIN_SPEED_PERCENT * 100.0,
              FlyingSwordBlockBreakTuning.BREAK_SCAN_RADIUS,
              FlyingSwordBlockBreakTuning.BREAK_MAX_BLOCKS_PER_TICK,
              sword.getSwordAttributes().toolTier,
              Math.max(0, extraTier),
              toolTier,
              tested,
              solid,
              mineableCount,
              eligible,
              broken));
    }
  }

  private static boolean isUnbreakable(ServerLevel level, BlockPos pos, BlockState state) {
    float h = state.getDestroySpeed(level, pos);
    return h < 0;
  }

  private static boolean canMine(BlockState state, int toolTier) {
    // pickaxe: honor NEEDS_* tags; axe/shovel: default tier 0 enough
    if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
      int required = 0;
      if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
        required = 3;
      } else if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
        required = 2;
      } else if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
        required = 1;
      }
      return toolTier >= required;
    }
    if (state.is(BlockTags.MINEABLE_WITH_AXE)
        || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
        || state.is(BlockTags.MINEABLE_WITH_HOE)
        || isHandBreakable(state)) {
      return true; // 最低等级即可
    }
    return false;
  }

  private static boolean isMineable(BlockState state) {
    return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
        || state.is(BlockTags.MINEABLE_WITH_AXE)
        || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
        || state.is(BlockTags.MINEABLE_WITH_HOE)
        || isHandBreakable(state);
  }

  private static boolean isHandBreakable(BlockState state) {
    // 不需要正确工具即可掉落时，视作“手可破”
    // 注：某些方块虽可徒手破坏但不掉落，这里以掉落逻辑为准，确保不卡住。
    return !state.requiresCorrectToolForDrops();
  }
}
