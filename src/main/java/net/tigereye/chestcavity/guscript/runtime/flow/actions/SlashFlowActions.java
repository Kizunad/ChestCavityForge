package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guscript.runtime.flow.SwordSlashConstants;

/**
 * 剑气（Slash）相关的方块破坏与伤害逻辑。
 */
final class SlashFlowActions {

    private static final double SLASH_DEFAULT_HEIGHT = 3.0D;
    private static final double SLASH_RAY_STEP = 1D;

    private SlashFlowActions() {
    }

    static FlowEdgeAction slashArea(double radius, double damage, double breakPower) {
        double r = Math.max(0.0D, radius);
        double dmg = Math.max(0.0D, damage);
        double bp = Math.max(0.0D, breakPower);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Level level = performer.level();
                if (!(level instanceof ServerLevel server)) {
                    return;
                }
                double resolvedDamage = dmg;
                if (controller != null) {
                    resolvedDamage = resolveDamageOverride(
                            controller,
                            SwordSlashConstants.FLOW_PARAM_DAMAGE_AREA,
                            resolvedDamage
                    );
                }
                slashBreakArea(server, performer, r, resolvedDamage, bp);
            }

            @Override
            public String describe() {
                return "slash_area(r=" + r + ", damage=" + dmg + ", break=" + bp + ")";
            }
        };
    }

    static FlowEdgeAction slashDelayedRay(int delayTicks, double length, double damage, double breakPower, double rayRadius) {
        int delay = Math.max(1, delayTicks);
        double len = Math.max(0.0D, length);
        double dmg = Math.max(0.0D, damage);
        double bp = Math.max(0.0D, breakPower);
        double radius = Math.max(0.1D, rayRadius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer.level() instanceof ServerLevel server)) {
                    return;
                }
                // 在施放瞬间锁定位置和朝向
                Vec3 start = performer.position();
                Vec3 direction = performer.getLookAngle().normalize();
                UUID performerId = performer.getUUID();

                double resolvedDamage = dmg;
                if (controller != null) {
                    resolvedDamage = resolveDamageOverride(
                            controller,
                            SwordSlashConstants.FLOW_PARAM_DAMAGE_RAY,
                            resolvedDamage
                    );
                }
                if resolvedDamage 
                final double damageForRay = resolvedDamage;
                Runnable task = () -> runLockedSlashRay(server, performerId, start, direction, len, damageForRay, bp, radius);
                if (controller != null) {
                    controller.schedule(gameTime + delay, task);
                } else {
                    task.run();
                }
            }


            @Override
            public String describe() {
                return "slash_delayed_ray(delay=" + delay + ", length=" + len + ")";
            }
        };
    }

    private static void slashBreakArea(ServerLevel server, Player performer, double radius, double damage, double breakPower) {
        HashSet<BlockPos> visited = new HashSet<>();
        
        Vec3 origin = performer.position();
        // 只取水平朝向
        Vec3 forward = performer.getLookAngle();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-6) {
            Vec3 fallbackForward = performer.getForward();
            horizontalForward = new Vec3(fallbackForward.x, 0.0D, fallbackForward.z);
        }
        Vec3 normalized = horizontalForward.normalize();

        double height = SLASH_DEFAULT_HEIGHT;

        // 实体伤害区域: 前方半圆 AABB
        Vec3 min = origin.add(-radius, -0.5D, -radius);
        Vec3 max = origin.add(radius, height, radius);
        AABB area = new AABB(min, max);
        slashDamageEntities(server, performer, area, damage);

        // 方块破坏：脚下为最低点，前方半圆
        BlockPos base = performer.blockPosition().below();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = (int) -radius; dx <= radius; dx++) {
            for (int dz = (int) -radius; dz <= radius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) {
                    continue; // 超出圆形
                }

                // 判断是否在前方 180°（点积大于等于 0）
                Vec3 delta = new Vec3(dx, 0, dz);
                if (delta.lengthSqr() > 1.0E-6) {
                    Vec3 deltaNorm = delta.normalize();
                    if (normalized.dot(deltaNorm) < 0) {
                        continue; // 在后方
                    }
                }

                // 高度循环
                for (int dy = 0; dy <= height; dy++) {
                    cursor.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (visited.add(cursor.immutable())) {
                        slashBreakBlock(server, performer, cursor, breakPower);
                    }
                }
            }
        }
    }


    private static void runSlashRay(ServerLevel server, UUID performerId, double length, double damage, double breakPower, double rayRadius) {
        HashSet<BlockPos> visited = new HashSet<>();
        
        Player performer = server.getPlayerByUUID(performerId);
        if (performer == null) {
            return;
        }
        // 起点：用眼睛位置更贴合射线
        Vec3 start = performer.position(); // 用脚底
        Vec3 direction = performer.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(length));

        // 实体检测：长方体通道 + 半径
        AABB area = new AABB(start, end).inflate(rayRadius);
        slashDamageEntities(server, performer, area, damage, start, end, rayRadius);

        // 方块破坏：圆柱形体积
        Vec3 step = direction.scale(SLASH_RAY_STEP);
        Vec3 current = start;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (double travelled = 0.0D; travelled <= length; travelled += SLASH_RAY_STEP) {
            int baseX = Mth.floor(current.x);
            int baseY = Mth.floor(current.y);
            int baseZ = Mth.floor(current.z);

            int intRadius = (int)Math.ceil(rayRadius);

            for (int dx = -intRadius; dx <= intRadius; dx++) {
                for (int dz = -intRadius; dz <= intRadius; dz++) {
                    if (dx * dx + dz * dz > rayRadius * rayRadius) {
                        continue; // 超出圆盘
                    }
                    for (int dy = -intRadius; dy <= intRadius; dy++) {
                        cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                        if (visited.add(cursor.immutable())) {
                            slashBreakBlock(server, performer, cursor, breakPower);
                        }
                    }
                }
            }

            current = current.add(step);
        }
    }


    private static boolean slashBreakBlock(ServerLevel server, Player performer, BlockPos pos, double breakPower) {
        if (!slashCanBreakBlocks()) {
            return false;
        }
        if (!server.isLoaded(pos)) {
            return false;
        }
        BlockState state = server.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (!isSlashBreakable(state)) {
            return false;
        }
        if (!server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !(performer instanceof Player)) {
            return false;
        }
        float hardness = state.getDestroySpeed(server, pos);
        if (hardness < 0.0F || hardness > breakPower) {
            return false;
        }
        return server.destroyBlock(pos, true, performer);
    }

    private static List<LivingEntity> slashDamageEntities(ServerLevel server, Player performer, AABB area, double damage) {
        return slashDamageEntities(server, performer, area, damage, null, null, 0.0D);
    }

    private static List<LivingEntity> slashDamageEntities(ServerLevel server, Player performer, AABB area, double damage, Vec3 rayStart, Vec3 rayEnd, double rayRadius) {
        List<LivingEntity> candidates = server.getEntitiesOfClass(LivingEntity.class, area, entity -> canDamageSlashTarget(performer, entity));
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<LivingEntity> affected = new ArrayList<>();
        for (LivingEntity entity : candidates) {
            if (rayStart != null && rayEnd != null && rayRadius > 0.0D) {
                double distanceSq = distanceToSegmentSquared(entity.getBoundingBox().getCenter(), rayStart, rayEnd);
                if (distanceSq > rayRadius * rayRadius) {
                    continue;
                }
            }
            if (!applySlashDamage(performer, entity, damage)) {
                continue;
            }
            Vec3 knock = performer.getLookAngle().normalize();
            if (knock.lengthSqr() > 1.0E-6) {
                entity.push(knock.x * 0.6D, 0.2D + Math.abs(knock.y) * 0.1D, knock.z * 0.6D);
                entity.hurtMarked = true;
            }
            affected.add(entity);
        }
        return affected;
    }

    private static boolean applySlashDamage(Player performer, LivingEntity entity, double damage) {
        if (damage <= 0.0D || entity == null) {
            return false;
        }
        float amount = (float) damage;
        boolean applied = false;
        try {
            entity.hurt(performer.damageSources().playerAttack(performer), amount);
            applied = true;
        } catch (Throwable ignored) {
        }
        if (!applied) {
            entity.setHealth(Math.max(0.0F, entity.getHealth() - amount));
            applied = true;
        }
        return applied;
    }

    private static boolean canDamageSlashTarget(Player performer, LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity == performer) {
            return false;
        }
        return !FlowActionUtils.isAlly(performer, entity);
    }

    private static boolean isSlashBreakable(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key != null && SwordSlashConstants.SLASH_BREAKABLE_IDS.contains(key)) {
            return true;
        }
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private static boolean slashCanBreakBlocks() {
        return ChestCavity.config != null
                && ChestCavity.config.SWORD_SLASH != null
                && ChestCavity.config.SWORD_SLASH.enableBlockBreaking;
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 ab = end.subtract(start);
        double lengthSq = ab.lengthSqr();
        if (lengthSq < 1.0E-6) {
            return point.distanceToSqr(start);
        }
        double t = point.subtract(start).dot(ab) / lengthSq;
        t = Mth.clamp(t, 0.0D, 1.0D);
        Vec3 projection = start.add(ab.scale(t));
        return point.distanceToSqr(projection);
    }

    // ✅ 新方法：在施法瞬间锁定起点与方向
    private static void runLockedSlashRay(ServerLevel server, UUID performerId,
                                    Vec3 start, Vec3 direction,
                                    double length, double damage, double breakPower, double rayRadius) {
        
        Player performer = server.getPlayerByUUID(performerId);
        if (performer == null) {
            ChestCavity.LOGGER.warn("[Slash] Scheduled ray skipped: performer {} missing", performerId);
            return;
        }

        Vec3 end = start.add(direction.scale(length));

        ChestCavity.LOGGER.info(
                "[Slash] Ray start performer={} damage={} breakPower={} length={} radius={} start=({}, {}, {}) dir=({}, {}, {})",
                performer.getGameProfile().getName(),
                formatDouble(damage),
                formatDouble(breakPower),
                formatDouble(length),
                formatDouble(rayRadius),
                formatDouble(start.x),
                formatDouble(start.y),
                formatDouble(start.z),
                formatDouble(direction.x),
                formatDouble(direction.y),
                formatDouble(direction.z)
        );

        // 实体检测
        AABB area = new AABB(start, end).inflate(rayRadius);
        List<LivingEntity> hits = slashDamageEntities(server, performer, area, damage, start, end, rayRadius);
        ChestCavity.LOGGER.info(
                "[Slash] Ray entity hits count={} names={}",
                hits.size(),
                hits.isEmpty()
                        ? "-"
                        : hits.stream().map(SlashFlowActions::describeEntity).collect(Collectors.joining(", "))
        );

        // 方块破坏逻辑
        int blocksBroken = 0;
        Vec3 step = direction.scale(SLASH_RAY_STEP);
        Vec3 current = start;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (double travelled = 0.0D; travelled <= length; travelled += SLASH_RAY_STEP) {
            int baseX = Mth.floor(current.x);
            int baseY = Mth.floor(current.y);
            int baseZ = Mth.floor(current.z);

            int intRadius = (int)Math.ceil(rayRadius);
            for (int dx = -intRadius; dx <= intRadius; dx++) {
                for (int dz = -intRadius; dz <= intRadius; dz++) {
                    if (dx * dx + dz * dz > rayRadius * rayRadius) continue;
                    for (int dy = -intRadius; dy <= intRadius; dy++) {
                        cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                        if (slashBreakBlock(server, performer, cursor, breakPower)) {
                            blocksBroken++;
                        }
                    }
                }
            }
            current = current.add(step);
        }

        ChestCavity.LOGGER.info(
                "[Slash] Ray finished performer={} blocksBroken={}"
                        + (blocksBroken > 0 ? "" : " (no breakable blocks)"),
                performer.getGameProfile().getName(),
                blocksBroken
        );
    }

    private static double resolveDamageOverride(FlowController controller, String key, double fallback) {
        if (controller == null || key == null) {
            return fallback;
        }
        String raw = controller.resolveFlowParam(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return fallback;
            }
            return parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String describeEntity(LivingEntity entity) {
        if (entity == null) {
            return "?";
        }
        String name = entity.getName().getString();
        if (name == null || name.isBlank()) {
            name = entity.getType().toShortString();
        }
        return name + "#" + entity.getId();
    }
}
