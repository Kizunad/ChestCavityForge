package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.IncomingThreat;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.InterceptPlanner;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.InterceptQuery;
import net.tigereye.chestcavity.registration.CCEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认护幕飞剑服务实现
 * <p>
 * 线程安全的单例实现，管理所有玩家的护幕飞剑。
 *
 * <h3>实现要点</h3>
 * <ul>
 *   <li>使用 ConcurrentHashMap 存储玩家 UUID → 护幕飞剑列表的映射</li>
 *   <li>ensureWardSwords() 自动创建或移除飞剑以达到目标数量</li>
 *   <li>tick() 驱动所有护幕飞剑的状态机</li>
 *   <li>onIncomingThreat() 分配拦截任务给最优飞剑</li>
 * </ul>
 */
public class DefaultWardSwordService implements WardSwordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWardSwordService.class);

    private static final DefaultWardSwordService INSTANCE = new DefaultWardSwordService();

    /**
     * 玩家UUID → 护幕飞剑列表
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<UUID, List<FlyingSwordEntity>> wardSwordMap = new ConcurrentHashMap<>();

    /**
     * 默认 WardTuning 实现
     */
    private DefaultWardTuning tuning = new DefaultWardTuning();

    private DefaultWardSwordService() {
        // 私有构造函数，单例模式
    }

    /**
     * 获取单例实例
     */
    public static DefaultWardSwordService getInstance() {
        return INSTANCE;
    }

    /**
     * 设置自定义的 WardTuning 实现
     */
    public void setTuning(DefaultWardTuning tuning) {
        this.tuning = tuning;
    }

    @Override
    public List<FlyingSwordEntity> ensureWardSwords(Player owner) {
        if (owner == null || !(owner.level() instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }

        UUID ownerId = owner.getUUID();

        // 获取当前护幕列表（如果不存在则创建）
        List<FlyingSwordEntity> currentSwords = wardSwordMap.computeIfAbsent(
            ownerId,
            k -> Collections.synchronizedList(new ArrayList<>())
        );

        // 清理已被移除的飞剑
        currentSwords.removeIf(sword -> sword == null || sword.isRemoved());

        // 获取目标数量
        int targetCount = tuning.maxSwords(ownerId);
        int currentCount = currentSwords.size();

        if (currentCount < targetCount) {
            // 需要创建新飞剑
            int toCreate = targetCount - currentCount;
            LOGGER.debug("Creating {} ward swords for player {}", toCreate, owner.getName().getString());

            for (int i = 0; i < toCreate; i++) {
                FlyingSwordEntity sword = createWardSword(owner, serverLevel, currentCount + i);
                if (sword != null) {
                    currentSwords.add(sword);
                }
            }
        } else if (currentCount > targetCount) {
            // 需要移除多余飞剑
            int toRemove = currentCount - targetCount;
            LOGGER.debug("Removing {} excess ward swords for player {}", toRemove, owner.getName().getString());

            for (int i = 0; i < toRemove; i++) {
                FlyingSwordEntity sword = currentSwords.remove(currentSwords.size() - 1);
                sword.discard();
            }
        }

        return new ArrayList<>(currentSwords);
    }

    /**
     * 创建一个新的护幕飞剑
     */
    private FlyingSwordEntity createWardSword(Player owner, ServerLevel level, int slotIndex) {
        try {
            // 创建飞剑实体
            FlyingSwordEntity sword = new FlyingSwordEntity(CCEntities.FLYING_SWORD.get(), level);

            // 设置所有者
            sword.setOwner(owner);

            // 标记为护幕飞剑
            sword.setWardSword(true);

            // 设置初始耐久
            double initialDurability = tuning.initialWardDurability(owner.getUUID());
            sword.setWardDurability(initialDurability);

            // 设置初始状态
            sword.setWardState(WardState.ORBIT);

            // 计算环绕槽位（相对位置）
            Vec3 orbitSlot = calculateOrbitSlot(slotIndex, getWardCount(owner));
            sword.setOrbitSlot(orbitSlot);

            // 设置初始位置（在主人周围）
            Vec3 absolutePos = owner.position().add(orbitSlot);
            sword.setPos(absolutePos);

            // 添加到世界
            level.addFreshEntity(sword);

            LOGGER.debug("Created ward sword {} for player {} at slot {}",
                sword.getId(), owner.getName().getString(), slotIndex);

            return sword;
        } catch (Exception e) {
            LOGGER.error("Failed to create ward sword for player {}", owner.getName().getString(), e);
            return null;
        }
    }

    /**
     * 计算环绕槽位的相对位置
     *
     * @param slotIndex 槽位索引（0, 1, 2, ...）
     * @param totalSlots 总槽位数
     * @return 相对于主人的位置向量
     */
    private Vec3 calculateOrbitSlot(int slotIndex, int totalSlots) {
        if (totalSlots <= 0) {
            totalSlots = 1;
        }

        // 环绕半径：r = 2.6 + 0.4 * N
        double radius = WardConfig.ORBIT_RADIUS_BASE + WardConfig.ORBIT_RADIUS_PER_SWORD * totalSlots;

        // 均匀分布在圆周上
        double angleStep = 2.0 * Math.PI / totalSlots;
        double angle = angleStep * slotIndex;

        // 计算相对位置（在XZ平面上环绕，Y轴偏移到眼睛高度）
        double x = radius * Math.cos(angle);
        double z = radius * Math.sin(angle);
        double y = 1.0; // 在主人眼睛高度附近

        return new Vec3(x, y, z);
    }

    @Override
    public void disposeWardSwords(Player owner) {
        if (owner == null) {
            return;
        }

        UUID ownerId = owner.getUUID();
        List<FlyingSwordEntity> swords = wardSwordMap.remove(ownerId);

        if (swords != null) {
            LOGGER.debug("Disposing {} ward swords for player {}", swords.size(), owner.getName().getString());
            for (FlyingSwordEntity sword : swords) {
                if (sword != null && !sword.isRemoved()) {
                    sword.discard();
                }
            }
        }
    }

    @Override
    public boolean onIncomingThreat(IncomingThreat threat) {
        if (threat == null || threat.target() == null) {
            return false;
        }

        if (!(threat.target() instanceof Player owner)) {
            return false;
        }

        List<FlyingSwordEntity> swords = getWardSwords(owner);

        if (swords.isEmpty()) {
            return false;
        }

        // 使用 InterceptPlanner 生成拦截查询
        InterceptQuery query = InterceptPlanner.plan(threat, owner, tuning);
        if (query == null) {
            // 无法在时间窗内拦截
            return false;
        }

        // 筛选可用的护幕飞剑（只有 ORBIT 状态的飞剑才能接受新任务）
        List<FlyingSwordEntity> availableSwords = new ArrayList<>();
        for (FlyingSwordEntity sword : swords) {
            if (sword.getWardState() == WardState.ORBIT) {
                availableSwords.add(sword);
            }
        }

        if (availableSwords.isEmpty()) {
            return false;
        }

        // 计算每个飞剑到达拦截点的时间，选择最快的
        FlyingSwordEntity bestSword = null;
        double minTime = Double.MAX_VALUE;

        for (FlyingSwordEntity sword : availableSwords) {
            double tReach = InterceptPlanner.timeToReach(sword, query.interceptPoint(), tuning);

            // 检查是否在时间窗内
            if (tReach >= tuning.windowMin() && tReach <= tuning.windowMax()) {
                if (tReach < minTime) {
                    minTime = tReach;
                    bestSword = sword;
                }
            }
        }

        if (bestSword == null) {
            return false;
        }

        // 分配拦截任务给最优飞剑
        bestSword.setWardState(WardState.INTERCEPT);
        bestSword.setCurrentQuery(query);
        bestSword.setInterceptStartTime(owner.level().getGameTime());

        LOGGER.debug("Assigned intercept task to sword {} (tReach={:.2f}s)",
            bestSword.getId(), minTime);

        return true;
    }

    @Override
    public void tick(Player owner) {
        if (owner == null || !owner.level().isClientSide) {
            // 只在服务端运行
            if (owner != null) {
                tickServerSide(owner);
            }
        }
    }

    /**
     * 服务端 Tick 逻辑
     */
    private void tickServerSide(Player owner) {
        // 更新玩家缓存，以便 WardTuning 能够访问玩家数据
        tuning.updatePlayerCache(owner);

        List<FlyingSwordEntity> swords = getWardSwords(owner);

        for (FlyingSwordEntity sword : swords) {
            if (sword == null || sword.isRemoved()) {
                continue;
            }

            // 委托给 tickWardSword 处理单个飞剑
            tickWardSword(sword, owner);
        }
    }

    @Override
    public void tickWardSword(FlyingSwordEntity sword, Player owner) {
        if (sword == null || sword.isRemoved() || owner == null) {
            return;
        }

        // 根据状态机执行对应行为
        switch (sword.getWardState()) {
            case ORBIT:
                tickOrbit(sword, owner);
                break;

            case INTERCEPT:
                tickIntercept(sword, owner);
                break;

            case COUNTER:
                tickCounter(sword, owner);
                break;

            case RETURN:
                tickReturn(sword, owner);
                break;
        }

        // 检查耐久是否耗尽
        if (sword.getWardDurability() <= 0) {
            LOGGER.debug("Ward sword {} durability depleted, removing", sword.getId());
            sword.discard();
        }
    }

    /**
     * ORBIT 状态：保持环绕位置
     */
    private void tickOrbit(FlyingSwordEntity sword, Player owner) {
        Vec3 orbitSlot = sword.getOrbitSlot();
        if (orbitSlot == null) {
            // 重新分配槽位
            int slotIndex = getWardSwords(owner).indexOf(sword);
            orbitSlot = calculateOrbitSlot(slotIndex, getWardCount(owner));
            sword.setOrbitSlot(orbitSlot);
        }

        // 计算目标位置（绝对坐标）
        Vec3 targetPos = owner.position().add(orbitSlot);

        // 转向目标
        double aMax = tuning.aMax(owner.getUUID());
        double vMax = tuning.vMax(owner.getUUID());
        sword.steerTo(targetPos, aMax, vMax);
    }

    /**
     * INTERCEPT 状态：向拦截点移动
     */
    private void tickIntercept(FlyingSwordEntity sword, Player owner) {
        InterceptQuery query = sword.getCurrentQuery();
        if (query == null) {
            // 没有拦截任务，返回 ORBIT
            sword.setWardState(WardState.RETURN);
            return;
        }

        // 向拦截点移动
        Vec3 targetPos = query.interceptPoint();
        double aMax = tuning.aMax(owner.getUUID());
        double vMax = tuning.vMax(owner.getUUID());
        sword.steerTo(targetPos, aMax, vMax);

        // 检查是否超时
        long currentTime = owner.level().getGameTime();
        long elapsed = currentTime - sword.getInterceptStartTime();
        double elapsedSeconds = elapsed / 20.0; // tick → 秒

        if (elapsedSeconds > tuning.windowMax()) {
            // 超时，拦截失败
            LOGGER.debug("Intercept timeout for sword {}", sword.getId());
            int costFail = tuning.costFail(owner.getUUID());
            sword.consumeWardDurability(costFail);
            sword.setWardState(WardState.RETURN);
            return;
        }

        // 检查是否到达拦截点（距离 < 0.5m）
        double distance = sword.position().distanceTo(targetPos);
        if (distance < WardConfig.INTERCEPT_SUCCESS_DISTANCE) {
            // 成功拦截
            LOGGER.debug("Intercept success for sword {}", sword.getId());
            int costBlock = tuning.costBlock(owner.getUUID());
            sword.consumeWardDurability(costBlock);

            // 检查是否触发反击
            IncomingThreat threat = query.threat();
            if (threat != null && threat.attacker() != null) {
                double attackerDistance = threat.attacker().position().distanceTo(owner.position());
                if (attackerDistance <= tuning.counterRange()) {
                    // 触发反击
                    sword.setWardState(WardState.COUNTER);
                    return;
                }
            }

            // 不触发反击，直接返回
            sword.setWardState(WardState.RETURN);
        }
    }

    /**
     * COUNTER 状态：执行反击
     */
    private void tickCounter(FlyingSwordEntity sword, Player owner) {
        InterceptQuery query = sword.getCurrentQuery();

        // 消耗反击耐久
        int costCounter = tuning.costCounter(owner.getUUID());
        sword.consumeWardDurability(costCounter);

        // 执行反击逻辑
        if (query != null && query.threat() != null) {
            IncomingThreat threat = query.threat();

            // 检查威胁类型：投射物 vs 近战
            if (threat.type() == IncomingThreat.Type.PROJECTILE) {
                // D.2: 投射物反弹
                performProjectileDeflection(threat, owner, sword);
            } else if (threat.type() == IncomingThreat.Type.MELEE) {
                // D.3: 近战反击（将在下一步实现）
                performMeleeCounter(threat, owner, sword);
            }
        }

        // 反击完成，返回环绕
        sword.setWardState(WardState.RETURN);
    }

    /**
     * 执行投射物反弹
     * <p>
     * 实现镜面反射：v' = v - 2*(v·n)*n
     * <ul>
     *   <li>计算反射速度</li>
     *   <li>改变投射物的所有者（如果可能）</li>
     *   <li>改变投射物速度</li>
     * </ul>
     *
     * @param threat 威胁对象
     * @param owner 玩家
     * @param sword 反击的飞剑
     */
    private void performProjectileDeflection(IncomingThreat threat, Player owner, FlyingSwordEntity sword) {
        // 获取投射物实体
        net.minecraft.world.entity.Entity directEntity = null;

        // 尝试从世界中找到投射物
        // 由于 IncomingThreat 中保存的是位置和速度，我们需要找到实际的投射物实体
        // 这里使用一个简单的方法：在附近查找投射物
        Vec3 threatPos = threat.position();
        if (owner.level() instanceof ServerLevel serverLevel) {
            // 在威胁位置附近查找投射物
            List<net.minecraft.world.entity.Entity> nearbyEntities = serverLevel.getEntities(
                owner,
                new net.minecraft.world.phys.AABB(threatPos, threatPos).inflate(2.0)
            );

            for (net.minecraft.world.entity.Entity entity : nearbyEntities) {
                if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
                    // 找到投射物，执行反弹
                    deflectProjectile(projectile, threat, owner, sword);
                    return;
                }
            }
        }

        LOGGER.debug("Could not find projectile to deflect at position {}", threatPos);
    }

    /**
     * 反弹投射物
     *
     * @param projectile 投射物实体
     * @param threat 威胁对象
     * @param owner 玩家
     * @param sword 反击的飞剑
     */
    private void deflectProjectile(
        net.minecraft.world.entity.projectile.Projectile projectile,
        IncomingThreat threat,
        Player owner,
        FlyingSwordEntity sword
    ) {
        Vec3 v = projectile.getDeltaMovement();

        // 计算反射法向量（从拦截点指向玩家的方向）
        Vec3 toOwner = owner.position().subtract(projectile.position()).normalize();

        // 镜面反射公式：v' = v - 2*(v·n)*n
        double vDotN = v.dot(toOwner);
        Vec3 vReflected = v.subtract(toOwner.scale(2.0 * vDotN));

        // 改变投射物速度
        projectile.setDeltaMovement(vReflected);

        // 尝试改变投射物的所有者为玩家
        try {
            if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
                // 对于箭矢，可以直接设置所有者
                arrow.setOwner(owner);
                LOGGER.debug("Deflected arrow, changed owner to player {}", owner.getName().getString());
            } else {
                // 对于其他投射物，尝试通用方法
                projectile.setOwner(owner);
                LOGGER.debug("Deflected projectile {}, changed owner to player {}",
                    projectile.getType().getDescription().getString(),
                    owner.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.warn("Could not change projectile owner: {}", e.getMessage());
        }

        LOGGER.debug("Projectile deflected with velocity {} -> {}", v, vReflected);
    }

    /**
     * 执行近战反击
     * <p>
     * 对攻击者造成反击伤害
     *
     * @param threat 威胁对象
     * @param owner 玩家
     * @param sword 反击的飞剑
     */
    private void performMeleeCounter(IncomingThreat threat, Player owner, FlyingSwordEntity sword) {
        if (!(threat.attacker() instanceof net.minecraft.world.entity.LivingEntity attacker)) {
            return;
        }

        // 计算反击伤害
        double damageAmount = tuning.counterDamage(owner.getUUID());

        // 创建伤害源：标记为来自玩家的反击
        // 使用飞剑作为直接来源，玩家作为实际来源
        net.minecraft.world.damagesource.DamageSource damageSource =
            owner.damageSources().mobAttack(owner);

        // 对攻击者造成伤害
        boolean damaged = attacker.hurt(damageSource, (float) damageAmount);

        if (damaged) {
            LOGGER.debug("Ward sword {} counter-attacked melee attacker {} with {} damage",
                sword.getId(),
                attacker.getName().getString(),
                damageAmount);

            // 可选：添加击退效果
            // 计算从玩家到攻击者的方向
            Vec3 knockbackDirection = attacker.position()
                .subtract(owner.position())
                .normalize()
                .scale(0.3); // 击退强度

            attacker.setDeltaMovement(
                attacker.getDeltaMovement().add(knockbackDirection.x, 0.2, knockbackDirection.z)
            );

            // 可选：生成粒子效果（表示"剑气突刺"）
            spawnCounterParticles(owner, attacker, sword);
        } else {
            LOGGER.debug("Ward sword {} counter-attack failed against {}",
                sword.getId(),
                attacker.getName().getString());
        }
    }

    /**
     * 生成反击粒子效果
     * <p>
     * 在玩家和攻击者之间生成粒子，表示"剑气突刺"
     *
     * @param owner 玩家
     * @param attacker 攻击者
     * @param sword 反击的飞剑
     */
    private void spawnCounterParticles(Player owner, net.minecraft.world.entity.LivingEntity attacker, FlyingSwordEntity sword) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 从玩家到攻击者的方向
        Vec3 start = owner.position().add(0, 1.0, 0);
        Vec3 end = attacker.position().add(0, 1.0, 0);
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.3);

        // 生成粒子轨迹
        for (double d = 0; d < distance; d += 0.3) {
            Vec3 particlePos = start.add(step.scale(d));

            // 使用剑气粒子（这里使用 SWEEP_ATTACK 粒子）
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                particlePos.x, particlePos.y, particlePos.z,
                1, // 粒子数量
                0.0, 0.0, 0.0, // 偏移
                0.0 // 速度
            );
        }

        LOGGER.debug("Spawned counter particles from {} to {}", start, end);
    }

    /**
     * RETURN 状态：返回环绕位
     */
    private void tickReturn(FlyingSwordEntity sword, Player owner) {
        Vec3 orbitSlot = sword.getOrbitSlot();
        if (orbitSlot == null) {
            // 重新分配槽位
            int slotIndex = getWardSwords(owner).indexOf(sword);
            orbitSlot = calculateOrbitSlot(slotIndex, getWardCount(owner));
            sword.setOrbitSlot(orbitSlot);
        }

        // 计算目标位置
        Vec3 targetPos = owner.position().add(orbitSlot);

        // 转向目标
        double aMax = tuning.aMax(owner.getUUID());
        double vMax = tuning.vMax(owner.getUUID());
        sword.steerTo(targetPos, aMax, vMax);

        // 检查是否已返回（距离 < 0.5m）
        double distance = sword.position().distanceTo(targetPos);
        if (distance < WardConfig.RETURN_SUCCESS_DISTANCE) {
            // 已返回，切换到 ORBIT
            LOGGER.debug("Sword {} returned to orbit", sword.getId());
            sword.setWardState(WardState.ORBIT);
        }
    }

    @Override
    public List<FlyingSwordEntity> getWardSwords(Player owner) {
        if (owner == null) {
            return Collections.emptyList();
        }

        UUID ownerId = owner.getUUID();
        List<FlyingSwordEntity> swords = wardSwordMap.get(ownerId);

        if (swords == null) {
            return Collections.emptyList();
        }

        // 清理已被移除的飞剑
        swords.removeIf(sword -> sword == null || sword.isRemoved());

        return new ArrayList<>(swords);
    }

    @Override
    public int getWardCount(Player owner) {
        return getWardSwords(owner).size();
    }

    @Override
    public boolean hasWardSwords(Player owner) {
        return getWardCount(owner) > 0;
    }
}
