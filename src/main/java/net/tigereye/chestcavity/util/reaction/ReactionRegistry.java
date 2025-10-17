package net.tigereye.chestcavity.util.reaction;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import org.slf4j.Logger;

import java.util.*;

/**
 * 反应系统（ReactionRegistry）
 * - 基于 DoT 类型标识与实体状态（status tag）触发化学/元素反应。
 * - 典型例：火衣 + 油涂层 -> 爆炸，并移除两者效果。
 *
 * 使用方式：
 * - ReactionRegistry.bootstrap() 于 mod 初始化调用，注册默认规则与 tick 清理。
 * - 调用 {@link #addStatus(LivingEntity, ResourceLocation, int)} 给实体附加临时状态（如 OIL_COATING）。
 * - DoTManager 在结算前调用 {@link #preApplyDoT(MinecraftServer, ResourceLocation, LivingEntity, LivingEntity)}，
 *   若返回 false 则本次 DoT 伤害被取消（用于“去除火衣”等）。
 */
public final class ReactionRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ReactionRegistry() {}

    // 每实体的临时状态：statusId -> expireTick
    private static final Map<UUID, Map<ResourceLocation, Long>> STATUSES = new HashMap<>();
    // 规则表：按 DoT typeId 分发
    private static final Map<ResourceLocation, List<ReactionRule>> RULES = new HashMap<>();
    // 火衣屏蔽：攻击者 UUID -> 截止 tick（在此之前屏蔽火衣 DoT 结算，可视为“去除火衣”）
    private static final Map<UUID, Long> FIRE_AURA_BLOCK_UNTIL = new HashMap<>();

    public static void bootstrap() {
        // 注册 tick 清理
        NeoForge.EVENT_BUS.addListener(ReactionRegistry::onServerTick);
        // 注册默认规则：火衣 + 油涂层 => 爆炸 + 移除油 + 短暂屏蔽火衣结算
        registerDefaults();
    }

    private static void registerDefaults() {
        // 规则：当触发 DoT 为 火衣光环，目标身上存在 OIL_COATING（切换至通用 Tag 判定）
        register(ReactionStatuses.OIL_COATING_TRIGGER_DOT,
                ctx -> net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(ctx.target(),
                        net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.OIL_COATING),
                ctx -> {
                    ServerLevel level = (ServerLevel) ctx.target().level();
                    double x = ctx.target().getX();
                    double y = ctx.target().getY() + ctx.target().getBbHeight() * 0.5;
                    double z = ctx.target().getZ();
                    float power = 1.8F + ReactionEvents.fireOilPowerBonus(ctx); // 适中爆炸，可被监听器调整
                    power = Math.max(0.5F, power);
                    try {
                        level.explode(null, x, y, z, power, Level.ExplosionInteraction.MOB);
                    } catch (Throwable t) {
                        LOGGER.warn("[reaction] explosion failed: {}", t.toString());
                    }
                    ReactionEvents.fireOil(ctx);
                    // 移除油涂层（通用 Tag 清理）
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(ctx.target(),
                            net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.OIL_COATING);
                    // 短暂屏蔽来自该攻击者的火衣 DoT（视为去除火衣），默认 3s
                    // 将屏蔽窗口设为 0（不做连锁抑制）
                    long now = ctx.server().getTickCount();
                    FIRE_AURA_BLOCK_UNTIL.put(ctx.attacker().getUUID(), now);
                    // 取消本次 DoT 伤害
                    return ReactionResult.cancel();
                });
    }

    public static void register(ResourceLocation triggerDotType,
                                ReactionPredicate predicate,
                                ReactionAction action) {
        RULES.computeIfAbsent(triggerDotType, k -> new ArrayList<>())
                .add(new ReactionRule(predicate, action));
    }

    // 在 DoT 伤害应用前调用；若返回 false，跳过本次伤害
    public static boolean preApplyDoT(MinecraftServer server,
                                      ResourceLocation dotTypeId,
                                      LivingEntity attacker,
                                      LivingEntity target) {
        if (server == null || dotTypeId == null || attacker == null || target == null) {
            return true;
        }
        long now = server.getTickCount();
        // 火衣屏蔽：若当前为火衣 DoT 且处于屏蔽窗口，则跳过
        if (ReactionStatuses.isHuoYiDot(dotTypeId)) {
            long until = FIRE_AURA_BLOCK_UNTIL.getOrDefault(attacker.getUUID(), 0L);
            if (until > now) {
                return false;
            }
        }
        List<ReactionRule> rules = RULES.get(dotTypeId);
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        ReactionContext ctx = new ReactionContext(server, dotTypeId, attacker, target);
        for (ReactionRule rule : rules) {
            try {
                if (rule.predicate().test(ctx)) {
                    ReactionResult result = rule.action().apply(ctx);
                    if (result != null && result.cancelDamage()) {
                        return false;
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("[reaction] rule failed for {}: {}", dotTypeId, t.toString());
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        // 清理过期状态（委托 TagOps）
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.purge(now);
        // 清理过期屏蔽
        FIRE_AURA_BLOCK_UNTIL.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private static void purgeStatuses(long now) {
        if (STATUSES.isEmpty()) return;
        Iterator<Map.Entry<UUID, Map<ResourceLocation, Long>>> it = STATUSES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<ResourceLocation, Long>> entry = it.next();
            Map<ResourceLocation, Long> map = entry.getValue();
            if (map == null || map.isEmpty()) {
                it.remove();
                continue;
            }
            map.entrySet().removeIf(e -> e.getValue() <= now);
            if (map.isEmpty()) {
                it.remove();
            }
        }
    }

    // -------- 状态 API（过渡：统一走 TagOps） --------

    public static void addStatus(LivingEntity entity, ResourceLocation statusId, int durationTicks) {
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(entity,
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId),
                durationTicks);
    }

    public static boolean hasStatus(LivingEntity entity, ResourceLocation statusId) {
        return net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(entity,
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId));
    }

    public static void clearStatus(LivingEntity entity, ResourceLocation statusId) {
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(entity,
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId));
    }

    /** 指定攻击者在给定时长内禁止触发火衣-油涂层反应。 */
    public static void blockFireOil(LivingEntity attacker, int durationTicks) {
        if (attacker == null || durationTicks <= 0) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel server)) {
            return;
        }
        long now = server.getServer().getTickCount();
        FIRE_AURA_BLOCK_UNTIL.put(attacker.getUUID(), now + durationTicks);
    }

    /** 当前是否处于火衣-油涂层反应的屏蔽窗口内。 */
    public static boolean isFireOilBlocked(LivingEntity attacker) {
        if (attacker == null) {
            return false;
        }
        if (!(attacker.level() instanceof ServerLevel server)) {
            return false;
        }
        long now = server.getServer().getTickCount();
        return FIRE_AURA_BLOCK_UNTIL.getOrDefault(attacker.getUUID(), 0L) > now;
    }

    // -------- 内部类型 --------

    public record ReactionContext(MinecraftServer server,
                                  ResourceLocation dotTypeId,
                                  LivingEntity attacker,
                                  LivingEntity target) {}

    public record ReactionResult(boolean cancelDamage) {
        public static ReactionResult proceed() { return new ReactionResult(false); }
        public static ReactionResult cancel() { return new ReactionResult(true); }
    }

    public record ReactionRule(ReactionPredicate predicate, ReactionAction action) {}

    @FunctionalInterface
    public interface ReactionPredicate { boolean test(ReactionContext ctx); }
    @FunctionalInterface
    public interface ReactionAction { ReactionResult apply(ReactionContext ctx); }
}
