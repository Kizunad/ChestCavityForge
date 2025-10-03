package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.combat.HunDaoDamageUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoBalance;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware.HunDaoMiddleware;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.state.SoulBeastStateManager;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.BeastSoulStorage;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.OrganBeastSoulStorage;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware.HunDaoMiddleware;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 小魂蛊（魂道）核心行为：
 * <p>
 * - 在玩家佩戴器官时：维持“活跃”状态、绑定归属者 UUID、确保联动通道存在；
 * - 在慢速心跳（onSlowTick）时：
 *   1) 持续消耗一定量魂魄（hunpo）；
 *   2) 通过进食接口维持饱食与饱和（防止战斗状态下过度饥饿）；
 *   3) 同步最近一次心跳时间到 NBT，便于状态调试与回溯；
 * - 在命中（onHit）时：
 *   1) 若魂魄足够，消耗一次性魂魄，按“最大魂魄值 × 百分比 × 联动效率”计算每秒 DoT；
 *   2) 通过 DoT 管理器为目标附加“魂焰”持续伤害若干秒；
 * - 在移除（onRemoved）时：保持“活跃态/绑定信息”，避免意外丢失归属绑定。
 * <p>
 * 关键 NBT 键：
 * - root: {@code HunDaoSoulBeast}
 * - {@code bound}：是否已与持有者绑定
 * - {@code active}：是否处于活跃态（装备后即置为 true）
 * - {@code owner_msb / owner_lsb}：持有者 UUID 的高/低位
 * - {@code bound_time}：绑定发生时的游戏时间
 * - {@code last_sync_tick}：上次慢速心跳写入时间（调试用）
 */
public final class HunDaoSoulBeastBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

    public static final HunDaoSoulBeastBehavior INSTANCE = new HunDaoSoulBeastBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUN_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect");

    private static final double PASSIVE_HUNPO_LEAK = 3.0;
    private static final double ATTACK_HUNPO_COST = 18.0;
    private static final double SOUL_FLAME_PERCENT = 0.01;
    private static final int SOUL_FLAME_DURATION_SECONDS = 5;

    private static final String STATE_ROOT_KEY = "HunDaoSoulBeast";
    private static final String KEY_BOUND = "bound";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_OWNER_MSB = "owner_msb";
    private static final String KEY_OWNER_LSB = "owner_lsb";
    private static final String KEY_BOUND_TIME = "bound_time";
    private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

    private final BeastSoulStorage beastSoulStorage = new OrganBeastSoulStorage(STATE_ROOT_KEY);

    private HunDaoSoulBeastBehavior() {
    }

    /**
     * 确保胸腔联动通道存在（用于从其他模块叠加效率增益）。
     * @param cc 胸腔实例
     */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
    }

    /**
     * 当器官被装备时：注册移除钩子、确保联动通道、绑定归属与活跃态，并同步一次槽位更新。
     */
    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        bindOrganState(cc, organ);
        sendSlotUpdate(cc, organ);
    }

    @Override
    /**
     * 慢速心跳：仅在服务端玩家触发。
     * - 维持活跃态；
     * - 被动魂魄泄露；
     * - 维持饱食饱和；
     * - 写入最近一次同步 tick。
     */
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        ensureAttached(cc);
        ensureActiveState(entity, organ);
        HunDaoMiddleware.INSTANCE.leakHunpoPerSecond(player, PASSIVE_HUNPO_LEAK);
        HunDaoMiddleware.INSTANCE.handlerPlayer(player);
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SYNC_TICK, state.setLong(KEY_LAST_SYNC_TICK, entity.level().getGameTime()));
    }

    @Override
    /**
     * 命中回调：尝试施加“魂焰”持续伤害。
     * 条件：
     * - 近战（排除弹射物）；
     * - 仅玩家为攻击者且在服务端；
     * - 拥有足够魂魄（ATTACK_HUNPO_COST）。
     * 计算：DoT = 最大魂魄 × SOUL_FLAME_PERCENT ×（1 + 联动增益）。
     */
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (source == null || !CombatEntityUtil.isMeleeHit(source) || target == null || !target.isAlive()) {
            return damage;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return damage;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        double currentHunpo = handle.read("hunpo").orElse(0.0);
        if (currentHunpo < ATTACK_HUNPO_COST) {
            LOGGER.debug("{} {} lacks hunpo for soul flame ({} / {})", prefix(), describePlayer(player), format(currentHunpo), format(ATTACK_HUNPO_COST));
            return damage;
        }
        handle.adjustDouble("hunpo", -ATTACK_HUNPO_COST, true, "zuida_hunpo");
        HunDaoDamageUtil.markHunDaoAttack(source);
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0);
        double efficiency = 1.0;
        if (cc != null) {
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            LinkageChannel channel = ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
            if (channel != null) {
                efficiency += Math.max(0.0, channel.get());
            }
        }
        double dotDamage = Math.max(0.0, maxHunpo * SOUL_FLAME_PERCENT * efficiency);
        if (dotDamage > 0.0) {
            HunDaoMiddleware.INSTANCE.applySoulFlame(player, target, dotDamage, SOUL_FLAME_DURATION_SECONDS);
            if (!(target instanceof Player)) {
                HunDaoMiddleware.INSTANCE.handlerNonPlayer(target);
            }
            LOGGER.debug("{} applied soul flame via middleware DoT={}s @{} to {}", prefix(), SOUL_FLAME_DURATION_SECONDS, format(dotDamage), target.getName().getString());
        }
        return damage;
    }

    @Override
    /**
     * 器官被移除时：维持活跃态与绑定信息，避免短时摘除导致状态丢失。
     */
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ensureActiveState(entity, organ);
        SoulBeastStateManager.setActive(player, true);
        LOGGER.debug("{} soul beast organ removed but state retained for {}", prefix(), describePlayer(player));
    }

    public BeastSoulStorage beastSoulStorage() {
        return beastSoulStorage;
    }

    /**
     * 初次装备时绑定归属信息并标记活跃态，写入绑定时间与持有者 UUID。
     */
    private void bindOrganState(ChestCavityInstance cc, ItemStack organ) {
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_BOUND, state.setBoolean(KEY_BOUND, true));
        if (cc.owner != null) {
            UUID ownerId = cc.owner.getUUID();
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_MSB, state.setLong(KEY_OWNER_MSB, ownerId.getMostSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_LSB, state.setLong(KEY_OWNER_LSB, ownerId.getLeastSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_BOUND_TIME, state.setLong(KEY_BOUND_TIME, cc.owner.level().getGameTime()));
        }
        logStateChange(LOGGER, prefix(), organ, KEY_ACTIVE, state.setBoolean(KEY_ACTIVE, true));
    }

    /**
     * 确保活跃态与归属 UUID 已写入；若缺失则以当前玩家补全。
     */
    private void ensureActiveState(LivingEntity entity, ItemStack organ) {
        if (!(entity instanceof Player player) || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_ACTIVE, state.setBoolean(KEY_ACTIVE, true));
        if (state.getLong(KEY_OWNER_MSB, 0L) == 0L && state.getLong(KEY_OWNER_LSB, 0L) == 0L) {
            UUID uuid = player.getUUID();
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_MSB, state.setLong(KEY_OWNER_MSB, uuid.getMostSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_LSB, state.setLong(KEY_OWNER_LSB, uuid.getLeastSignificantBits()));
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(organ.getItem());
        SoulBeastStateManager.setActive(player, true);
        SoulBeastStateManager.setSource(player, itemId);
    }

    // 资源维护与 DoT 已解耦至 HunDaoMiddleware

    /**
     * 简要描述玩家（记日志用）。
     */
    private String describePlayer(Player player) {
        return player.getScoreboardName();
    }

    /**
     * 数值格式化为两位小数（日志用）。
     */
    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * 统一日志前缀。
     */
    private String prefix() {
        return "[compat/guzhenren][hun_dao]";
    }
}
