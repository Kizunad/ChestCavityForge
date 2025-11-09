package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认护幕数值供给实现
 * <p>
 * D 阶段：集成道痕与流派经验参数，动态计算护幕属性。
 * <p>
 * 使用 {@link GuzhenrenResourceBridge} 读取玩家的剑道道痕和剑道流派经验。
 *
 * <h3>实现策略</h3>
 * <ul>
 *   <li>所有方法使用 {@link WardConfig} 中的常量作为基准值</li>
 *   <li>公式根据玩家的道痕和流派经验动态计算</li>
 *   <li>支持后续扩展为配置文件驱动实现</li>
 * </ul>
 */
public class DefaultWardTuning implements WardTuning {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWardTuning.class);

    /**
     * 默认道痕等级（当无法读取时使用）
     */
    private static final double DEFAULT_TRAIL_LEVEL = 0.0;

    /**
     * 默认流派经验（当无法读取时使用）
     */
    private static final double DEFAULT_SECT_EXP = 0.0;

    /**
     * 玩家数据缓存（UUID → 玩家实例）
     * <p>
     * 用于从 UUID 反查 Player 对象
     */
    private final ConcurrentHashMap<UUID, Player> playerCache = new ConcurrentHashMap<>();

    @Override
    public int maxSwords(UUID owner) {
        // 公式：N = clamp(1 + floor(sqrt(道痕/100)) + floor(经验/1000), 1, max)
        // 骨架阶段：返回固定值
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        int n = 1 + (int) Math.floor(Math.sqrt(trail / 100.0)) + (int) Math.floor(exp / 1000.0);
        return Math.max(1, Math.min(n, WardConfig.MAX_WARDS));
    }

    @Override
    public double orbitRadius(UUID owner, int currentSwordCount) {
        // 公式：r = 2.6 + 0.4 * N
        return WardConfig.ORBIT_RADIUS_BASE + WardConfig.ORBIT_RADIUS_PER_SWORD * currentSwordCount;
    }

    @Override
    public double vMax(UUID owner) {
        // 公式：vMax = 6.0 + 0.02 * 道痕 + 0.001 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.SPEED_BASE
            + WardConfig.SPEED_TRAIL_COEF * trail
            + WardConfig.SPEED_EXP_COEF * exp;
    }

    @Override
    public double aMax(UUID owner) {
        // 骨架阶段：返回常量
        return WardConfig.ACCEL_BASE;
    }

    @Override
    public double reactionDelay(UUID owner) {
        // 公式：reaction = clamp(0.06 - 0.00005 * 经验, 0.02, 0.06)
        double exp = getSectExperience(owner);

        double reaction = WardConfig.REACTION_BASE - WardConfig.REACTION_EXP_COEF * exp;
        return Math.max(WardConfig.REACTION_MIN, Math.min(reaction, WardConfig.REACTION_MAX));
    }

    @Override
    public double counterRange() {
        return WardConfig.COUNTER_RANGE;
    }

    @Override
    public double windowMin() {
        return WardConfig.WINDOW_MIN;
    }

    @Override
    public double windowMax() {
        return WardConfig.WINDOW_MAX;
    }

    @Override
    public int costBlock(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costBlock = round(8 * (1 - R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_BLOCK * (1.0 - r));
    }

    @Override
    public int costCounter(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costCounter = round(10 * (1 - R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_COUNTER * (1.0 - r));
    }

    @Override
    public int costFail(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costFail = round(2 * (1 - 0.5*R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_FAIL * (1.0 - 0.5 * r));
    }

    @Override
    public double counterDamage(UUID owner) {
        // 公式：D = 5.0 + 0.05 * 道痕 + 0.01 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.COUNTER_DAMAGE_BASE
            + WardConfig.COUNTER_DAMAGE_TRAIL_COEF * trail
            + WardConfig.COUNTER_DAMAGE_EXP_COEF * exp;
    }

    @Override
    public double initialWardDurability(UUID owner) {
        // 公式：Dur0 = 60 + 0.3 * 道痕 + 0.1 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.INITIAL_DUR_BASE
            + WardConfig.INITIAL_DUR_TRAIL * trail
            + WardConfig.INITIAL_DUR_EXP * exp;
    }

    // ====== 辅助方法（D 阶段：集成 GuzhenRen API）======

    /**
     * 更新玩家缓存
     * <p>
     * 用于从 UUID 反查 Player 对象。
     * 应该在每次 tick 或操作时调用，以确保缓存是最新的。
     *
     * @param player 玩家实例
     */
    public void updatePlayerCache(Player player) {
        if (player != null) {
            playerCache.put(player.getUUID(), player);
        }
    }

    /**
     * 从 UUID 获取玩家实例
     *
     * @param owner 玩家 UUID
     * @return 玩家实例，如果未找到则返回 null
     */
    protected Player getPlayer(UUID owner) {
        return playerCache.get(owner);
    }

    /**
     * 获取玩家道痕等级
     * <p>
     * D 阶段：使用 {@link GuzhenrenResourceBridge} 读取剑道道痕
     *
     * @param owner 玩家 UUID
     * @return 道痕等级（剑道道痕值）
     */
    protected double getTrailLevel(UUID owner) {
        Player player = getPlayer(owner);
        if (player == null) {
            return DEFAULT_TRAIL_LEVEL;
        }

        // 检查 GuzhenRen 桥接是否可用
        if (!GuzhenrenResourceBridge.isAvailable()) {
            return DEFAULT_TRAIL_LEVEL;
        }

        try {
            return GuzhenrenResourceBridge.open(player)
                .map(handle -> handle.read("daohen_jiandao").orElse(DEFAULT_TRAIL_LEVEL))
                .orElse(DEFAULT_TRAIL_LEVEL);
        } catch (Exception e) {
            LOGGER.warn("Failed to read trail level for player {}: {}",
                owner, e.getMessage());
            return DEFAULT_TRAIL_LEVEL;
        }
    }

    /**
     * 获取玩家流派经验
     * <p>
     * D 阶段：使用 {@link GuzhenrenResourceBridge} 读取剑道流派经验
     *
     * @param owner 玩家 UUID
     * @return 流派经验（剑道流派经验值）
     */
    protected double getSectExperience(UUID owner) {
        Player player = getPlayer(owner);
        if (player == null) {
            return DEFAULT_SECT_EXP;
        }

        // 检查 GuzhenRen 桥接是否可用
        if (!GuzhenrenResourceBridge.isAvailable()) {
            return DEFAULT_SECT_EXP;
        }

        try {
            return GuzhenrenResourceBridge.open(player)
                .map(handle -> handle.read("liupai_jiandao").orElse(DEFAULT_SECT_EXP))
                .orElse(DEFAULT_SECT_EXP);
        } catch (Exception e) {
            LOGGER.warn("Failed to read sect experience for player {}: {}",
                owner, e.getMessage());
            return DEFAULT_SECT_EXP;
        }
    }
}
