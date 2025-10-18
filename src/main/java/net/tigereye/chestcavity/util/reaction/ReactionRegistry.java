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
import net.tigereye.chestcavity.config.CCConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

import java.util.*;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

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
    // 火×腐蚀 限流：攻击者 -> (lastTick,count)
    private static final Map<UUID, Integer> FIRE_CORROSION_LAST_TICK = new HashMap<>();
    private static final Map<UUID, Integer> FIRE_CORROSION_COUNT = new HashMap<>();

    public static void bootstrap() {
        // 注册 tick 清理
        NeoForge.EVENT_BUS.addListener(ReactionRegistry::onServerTick);
        // 注册默认规则：火衣 + 油涂层 => 爆炸 + 移除油 + 短暂屏蔽火衣结算
        registerDefaults();
        // 注册元素规则（冰/魂/腐蚀/联动）
        registerElementalDefaults();
        // 注册血道规则（血印/失血 与 各系 DoT 的联动）
        registerBloodDefaults();
        // 注册毒道/骨道等扩展规则
        registerToxicDefaults();
        registerBoneDefaults();
    }

    private static void registerToxicDefaults() {
        // 火衣 × 臭云（STENCH_CLOUD）=> 毒燃闪爆
        register(net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.STENCH_CLOUD)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.TOXIC_IMMUNE),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    if (ctx.target().level() instanceof ServerLevel level) {
                        float r = Math.max(1.5F, 2.0F);
                        double dmg = Math.max(2.0D, 3.5D);
                        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueAoEDamage(
                                level, ctx.target().getX(), ctx.target().getY(), ctx.target().getZ(), r, dmg, ctx.attacker(),
                                net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.FIRE);
                    }
                    ReactionTagOps.clear(ctx.target(), ReactionTagKeys.STENCH_CLOUD);
                    ReactionTagOps.add(ctx.target(), ReactionTagKeys.TOXIC_IMMUNE, 40);
                    String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "火衣";
                    String t = ctx.target().getName().getString();
                    i18nMessage(ctx.attacker(), "message.chestcavity.reaction.toxic_flash.attacker", t);
                    i18nMessage(ctx.target(), "message.chestcavity.reaction.toxic_flash.target", a);
                    return ReactionResult.proceed();
                });

        // 霜焰 DoT × 臭云 => 凝霜毒晶（取消本次毒蒸发，改为直伤+短冻结/致盲）
        register(net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.STENCH_CLOUD)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
                ctx -> {
                    LivingEntity target = ctx.target();
                    LivingEntity attacker = ctx.attacker();
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), 3.0F);
                    } else {
                        target.hurt(target.damageSources().generic(), 3.0F);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
                    ReactionTagOps.clear(target, ReactionTagKeys.STENCH_CLOUD);
                    ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, 40);
                    String a = attacker != null ? attacker.getName().getString() : "霜";
                    String t = target.getName().getString();
                    i18nMessage(attacker, "message.chestcavity.reaction.toxic_frost_crystal.attacker", t);
                    i18nMessage(target, "message.chestcavity.reaction.toxic_frost_crystal.target", a);
                    return ReactionResult.cancel();
                });
    }

    private static void registerBoneDefaults() {
        // 预留骨系规则（后续在各骨系行为挂 BONE_MARK/SHARD_FIELD 后生效）
        register(net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_MARK),
                ctx -> {
                    LivingEntity target = ctx.target();
                    LivingEntity attacker = ctx.attacker();
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), 2.0F);
                    } else {
                        target.hurt(target.damageSources().generic(), 2.0F);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, true));
                    ReactionTagOps.clear(target, ReactionTagKeys.BONE_MARK);
                    i18nMessage(attacker, "message.chestcavity.reaction.bone_corrosion_crack.attacker", target.getName().getString());
                    i18nMessage(target, "message.chestcavity.reaction.bone_corrosion_crack.target", attacker != null ? attacker.getName().getString() : "腐蚀");
                    return ReactionResult.proceed();
                });
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
                    // 按配置执行：oil 专用 AoE 或强制爆炸
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    if (C.fireOilUseAoE) {
                        float r = Math.max(0.5F, power * 1.6F);
                        double dmg = power * 2.0F;
                        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueAoEDamage(level, x, y, z, r, dmg, null,
                                net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.FIRE);
                    } else {
                        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueExplosion(level, x, y, z, power, null, true,
                                net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.FIRE);
                    }
                    ReactionEvents.fireOil(ctx);
                    // 移除油涂层（通用 Tag 清理）
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(ctx.target(),
                            net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.OIL_COATING);
                    // 短暂屏蔽来自该攻击者的火衣 DoT（视为去除火衣），默认 3s
                    // 将屏蔽窗口设为 0（不做连锁抑制）
                    long now = ctx.server().getTickCount();
                    FIRE_AURA_BLOCK_UNTIL.put(ctx.attacker().getUUID(), now);
                    // 取消本次 DoT 伤害，提示
                    {
                        String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "火衣";
                        String t = ctx.target().getName().getString();
                        i18nMessage(ctx.attacker(), "message.chestcavity.reaction.fire_oil.attacker", t);
                        i18nMessage(ctx.target(), "message.chestcavity.reaction.fire_oil.target", a);
                    }
                    return ReactionResult.cancel();
                });
    }

    // 血道联动规则（血印/失血等）
    private static void registerBloodDefaults() {
        // 常量：通用免疫时长（防抖）
        final int IMMUNE_SHORT = 40; // 2秒

        // 沸血（火衣 × 血印/失血）
        register(net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
                ctx -> (ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
                        || ReactionTagOps.has(ctx.target(), ReactionTagKeys.HEMORRHAGE))
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
                ctx -> {
                    LivingEntity attacker = ctx.attacker();
                    LivingEntity target = ctx.target();
                    double bonus = 3.0D;
                    // 血怒/血誓期间给予小幅加成
                    if (ReactionTagOps.has(attacker, ReactionTagKeys.BLOOD_RAGE)) bonus += 2.0D;
                    if (ReactionTagOps.has(attacker, ReactionTagKeys.BLOOD_OATH)) bonus += 2.0D;
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), (float) Math.max(0.0D, bonus));
                    } else {
                        target.hurt(target.damageSources().generic(), (float) Math.max(0.0D, bonus));
                    }
                    // 清除血印，短暂火系免疫，提示
                    ReactionTagOps.clear(target, ReactionTagKeys.BLOOD_MARK);
                    ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
                    {
                        String a = attacker != null ? attacker.getName().getString() : "火衣";
                        String t = target.getName().getString();
                        i18nMessage(attacker, "message.chestcavity.reaction.blood_boil.attacker", t);
                        i18nMessage(target, "message.chestcavity.reaction.blood_boil.target", a);
                    }
                    return ReactionResult.proceed();
                });

        // 凝血碎裂（霜痕 × 血印）
        register(net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
                ctx -> {
                    LivingEntity attacker = ctx.attacker();
                    LivingEntity target = ctx.target();
                    // 一次性小额真实/通用伤 + 减速
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().generic(), 4.0F);
                    } else {
                        target.hurt(target.damageSources().generic(), 4.0F);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                    ReactionTagOps.clear(target, ReactionTagKeys.BLOOD_MARK);
                    ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, IMMUNE_SHORT);
                    {
                        String a = attacker != null ? attacker.getName().getString() : "寒息";
                        String t = target.getName().getString();
                        i18nMessage(attacker, "message.chestcavity.reaction.blood_coag_burst.attacker", t);
                        i18nMessage(target, "message.chestcavity.reaction.blood_coag_burst.target", a);
                    }
                    return ReactionResult.proceed();
                });

        // 渗魂回声（魂焰 × 血印）
        register(net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
                ctx -> {
                    // 延迟一次魂伤
                    scheduleDelayedDamage(ctx.server(), ctx.attacker(), ctx.target(), 6.0D, 40);
                    ReactionTagOps.clear(ctx.target(), ReactionTagKeys.BLOOD_MARK);
                    ReactionTagOps.add(ctx.target(), ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
                    {
                        String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "魂焰";
                        String t = ctx.target().getName().getString();
                        i18nMessage(ctx.attacker(), "message.chestcavity.reaction.blood_echo.attacker", t);
                        i18nMessage(ctx.target(), "message.chestcavity.reaction.blood_echo.target", a);
                    }
                    return ReactionResult.proceed();
                });

        // 败血激增（腐蚀 × 血印）
        register(net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
                ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_IMMUNE),
                ctx -> {
                    LivingEntity attacker = ctx.attacker();
                    LivingEntity target = ctx.target();
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), 3.0F);
                    } else {
                        target.hurt(target.damageSources().generic(), 3.0F);
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
                    if (target.level() instanceof ServerLevel level) {
                        // 简易残留域（弱化版）
                        spawnResidueCorrosion(level, target.getX(), target.getY(), target.getZ(), 2.0F, 80);
                    }
                    ReactionTagOps.clear(target, ReactionTagKeys.BLOOD_MARK);
                    ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
                    {
                        String a = attacker != null ? attacker.getName().getString() : "腐蚀";
                        String t = target.getName().getString();
                        i18nMessage(attacker, "message.chestcavity.reaction.blood_septic.attacker", t);
                        i18nMessage(target, "message.chestcavity.reaction.blood_septic.target", a);
                    }
                    return ReactionResult.proceed();
                });
    }

    private static void registerElementalDefaults() {
        // 霜痕碎裂（FROST_SHATTER）
        register(net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
                ctx -> ChestCavity.config.REACTION.enableFrost
                        && ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    LivingEntity target = ctx.target();
                    LivingEntity attacker = ctx.attacker();
                    // 取消本次 DoT，执行一次直伤 + 击退
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), (float) Math.max(0.0D, C.frostShatterDamage));
                    } else {
                        target.hurt(target.damageSources().generic(), (float) Math.max(0.0D, C.frostShatterDamage));
                    }
                    // 击退
                    try {
                        if (attacker != null) {
                            var push = target.position().subtract(attacker.position()).normalize().scale(Math.max(0.0D, C.frostShatterKnockback));
                            target.push(push.x, 0.02D, push.z);
                            target.hurtMarked = true;
                        }
                    } catch (Throwable ignored) {}
                    // 残留概率生成（缓慢云）→ 交给 ReactionEngine 合并/刷新
                    if (target.level() instanceof ServerLevel level) {
                        if (level.getRandom().nextDouble() < Math.max(0.0D, Math.min(1.0D, C.frostResidueChance))) {
                            net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueFrostResidue(
                                    level, target.getX(), target.getY(), target.getZ(), C.frostResidueRadius, C.frostResidueDurationTicks, C.frostResidueSlowAmplifier);
                        }
                    }
                    ReactionTagOps.clear(target, ReactionTagKeys.FROST_MARK);
                    ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, Math.max(0, C.frostImmuneTicks));
                    {
                        String a = attacker != null ? attacker.getName().getString() : "环境";
                        String t = target.getName().getString();
                        i18nMessage(attacker, "message.chestcavity.reaction.frost_shatter.attacker", t);
                        i18nMessage(target, "message.chestcavity.reaction.frost_shatter.target", a);
                    }
                    return ReactionResult.cancel();
                });

        // 蒸汽灼烫（火×冰）
        register(net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
                ctx -> ChestCavity.config.REACTION.enableSteamScald
                        && ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_MARK),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    if (ctx.target().level() instanceof ServerLevel level) {
                        float radius = Math.max(0.5F, C.steamScaldRadius);
                        double dmg = Math.max(0.0D, C.steamScaldDamage);
                        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueAoEDamage(
                                level, ctx.target().getX(), ctx.target().getY(), ctx.target().getZ(), radius, dmg, ctx.attacker(),
                                net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.STEAM);
                        // 目标本身立刻附加失明，范围内其他实体的 Debuff 如需可在 Engine 侧扩展
                        ctx.target().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.max(0, C.steamScaldBlindnessTicks), 0, false, true));
                    }
                    ReactionTagOps.clear(ctx.target(), ReactionTagKeys.FROST_MARK);
                    {
                        String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "火衣";
                        String t = ctx.target().getName().getString();
                        i18nMessage(ctx.attacker(), "message.chestcavity.reaction.steam_scald.attacker", t);
                        i18nMessage(ctx.target(), "message.chestcavity.reaction.steam_scald.target", a);
                    }
                    return ReactionResult.proceed();
                });

        // 魂印回声（延迟 DoT）
        register(net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
                ctx -> ChestCavity.config.REACTION.enableSoulEcho
                        && ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    scheduleDelayedDamage(ctx.server(), ctx.attacker(), ctx.target(), Math.max(0.0D, C.soulEchoDamage), Math.max(0, C.soulEchoDelayTicks));
                    ReactionTagOps.clear(ctx.target(), ReactionTagKeys.SOUL_MARK);
                    ReactionTagOps.add(ctx.target(), ReactionTagKeys.SOUL_IMMUNE, Math.max(0, C.soulImmuneTicks));
                    {
                        int dt = Math.max(0, C.soulEchoDelayTicks);
                        String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "未知";
                        String t = ctx.target().getName().getString();
                        String secs = String.format(Locale.ROOT, "%.1f", dt/20.0);
                        i18nMessage(ctx.attacker(), "message.chestcavity.reaction.soul_echo.attacker", secs, t);
                        i18nMessage(ctx.target(), "message.chestcavity.reaction.soul_echo.target", a);
                    }
                    return ReactionResult.proceed(); // 本次魂焰 DoT 照常结算
                });

        // 腐蚀激增
        register(net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
                ctx -> ChestCavity.config.REACTION.enableCorrosionSurge
                        && ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_IMMUNE),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    LivingEntity target = ctx.target();
                    LivingEntity attacker = ctx.attacker();
                    // 额外伤害 + 虚弱
                    if (attacker != null) {
                        target.hurt(attacker.damageSources().mobAttack(attacker), (float) Math.max(0.0D, C.corrosionSurgeBonusDamage));
                    } else {
                        target.hurt(target.damageSources().generic(), (float) Math.max(0.0D, C.corrosionSurgeBonusDamage));
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(0, C.corrosionSurgeWeaknessTicks), 0, false, true));
                    if (target.level() instanceof ServerLevel level) {
                        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueCorrosionResidue(
                                level, target.getX(), target.getY(), target.getZ(), C.corrosionResidueRadius, C.corrosionResidueDurationTicks);
                    }
                    ReactionTagOps.clear(target, ReactionTagKeys.CORROSION_MARK);
                    ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, Math.max(0, C.corrosionImmuneTicks));
                    {
                        String a = attacker != null ? attacker.getName().getString() : "环境";
                        String t = target.getName().getString();
                        i18nMessage(attacker, "message.chestcavity.reaction.corrosion_surge.attacker", t);
                        i18nMessage(target, "message.chestcavity.reaction.corrosion_surge.target", a);
                    }
                    return ReactionResult.proceed();
                });

        // 火×腐蚀（小爆）
        register(net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
                ctx -> ChestCavity.config.REACTION.enableFireCorrosion
                        && ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_MARK)
                        && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
                ctx -> {
                    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
                    if (ctx.target().level() instanceof ServerLevel level) {
                        if (C.fireCorrosionUseAoE) {
                            net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueAoEDamage(
                                    level, ctx.target().getX(), ctx.target().getY(), ctx.target().getZ(),
                                    Math.max(0.5F, C.fireCorrosionAoERadius), Math.max(0.0D, C.fireCorrosionAoEDamage), ctx.attacker(),
                                    net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.FIRE);
                        } else {
                            float power = Math.max(0.1F, Math.min(C.fireCorrosionExplosionPower, C.fireCorrosionMaxPower));
                            net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueExplosion(
                                    level, ctx.target().getX(), ctx.target().getY(), ctx.target().getZ(), power, ctx.attacker(), false,
                                    net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.FIRE);
                        }
                    }
                    ReactionTagOps.clear(ctx.target(), ReactionTagKeys.CORROSION_MARK);
                    // 添加短 CD 防抖，避免频繁引爆导致卡顿
                    ReactionTagOps.add(ctx.target(), ReactionTagKeys.FIRE_IMMUNE, Math.max(0, C.fireCorrosionCooldownTicks));
                    {
                        String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "火衣";
                        String t = ctx.target().getName().getString();
                        i18nMessage(ctx.attacker(), "message.chestcavity.reaction.fire_corrosion.attacker", t);
                        i18nMessage(ctx.target(), "message.chestcavity.reaction.fire_corrosion.target", a);
                    }
                    // 取消本次火衣 DoT 伤害，避免同一tick重复伤害引发事件总线开销
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
        // 处理延迟伤害
        if (!DELAYED.isEmpty()) {
            Iterator<DelayedDamage> it = DELAYED.iterator();
            while (it.hasNext()) {
                DelayedDamage d = it.next();
                if (d.dueTick <= now) {
                    applyDelayedDamage(event.getServer(), d);
                    it.remove();
                }
            }
        }
        // 统一执行 ReactionEngine 中排队的 AoE/Explode/Residue 任务（带限流/降级）
        net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.process(event.getServer());
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

    // -------- Delayed damage (for Soul Echo) --------
    private static final List<DelayedDamage> DELAYED = new ArrayList<>();
    private record DelayedDamage(int dueTick, UUID attacker, UUID target, float amount) {}

    private static void scheduleDelayedDamage(MinecraftServer server, LivingEntity attacker, LivingEntity target, double amount, int delayTicks) {
        if (server == null || target == null || delayTicks < 0) return;
        int due = server.getTickCount() + delayTicks;
        UUID a = attacker != null ? attacker.getUUID() : null;
        UUID t = target.getUUID();
        DELAYED.add(new DelayedDamage(due, a, t, (float) amount));
    }

    private static void applyDelayedDamage(MinecraftServer server, DelayedDamage d) {
        if (d == null) return;
        LivingEntity target = findLiving(server, d.target);
        if (target == null || !target.isAlive()) return;
        LivingEntity attacker = d.attacker != null ? findLiving(server, d.attacker) : null;
        if (attacker != null && target.isAlliedTo(attacker)) return;
        if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), d.amount);
        } else {
            target.hurt(target.damageSources().generic(), d.amount);
        }
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof LivingEntity le) return le;
        }
        return null;
    }

    // -------- Residue helpers --------
    private static void spawnResidueFrost(ServerLevel level, double x, double y, double z, float radius, int durationTicks, int slowAmplifier) {
        try {
            AreaEffectCloud aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            aec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, slowAmplifier), false, true));
            level.addFreshEntity(aec);
        } catch (Throwable ignored) {}
    }

    private static void spawnResidueCorrosion(ServerLevel level, double x, double y, double z, float radius, int durationTicks) {
        try {
            AreaEffectCloud aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            aec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
            level.addFreshEntity(aec);
        } catch (Throwable ignored) {}
    }

    private static void debugMessage(LivingEntity viewer, String text) {
        if (viewer instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.literal(text));
        }
    }

    // 使用本地化键发送系统消息（优先用于调试/提示文案），以便与 guzhenren 风格保持一致
    private static void i18nMessage(LivingEntity viewer, String key, Object... args) {
        if (viewer instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(Component.translatable(key, args));
        }
    }
}
