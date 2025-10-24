package net.tigereye.chestcavity.util.reaction;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.fx.HuoLongFx;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.engine.TickEngineHub;
import net.tigereye.chestcavity.engine.reaction.DelayedDamageEngine;
import net.tigereye.chestcavity.engine.reaction.EffectsEngine;
import net.tigereye.chestcavity.engine.reaction.ReactionRuntime;
import net.tigereye.chestcavity.engine.reaction.ResidueManager;
import net.tigereye.chestcavity.util.reaction.api.DefaultReactionService;
import net.tigereye.chestcavity.util.reaction.api.ReactionAPI;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/**
 * 反应系统（ReactionRegistry） - 基于 DoT 类型标识与实体状态（status tag）触发化学/元素反应。 - 典型例：火衣 + 油涂层 -> 爆炸，并移除两者效果。
 *
 * <p>使用方式： - ReactionRegistry.bootstrap() 于 mod 初始化调用，注册默认规则与 tick 清理。 - 调用 {@link
 * #addStatus(LivingEntity, ResourceLocation, int)} 给实体附加临时状态（如 OIL_COATING）。 - DoTEngine 在结算前调用
 * {@link #preApplyDoT(MinecraftServer, ResourceLocation, LivingEntity, LivingEntity)}， 若返回 false
 * 则本次 DoT 伤害被取消（用于“去除火衣”等）。
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
  private static final Map<UUID, Long> DRAGON_FLAME_IGNITE_LAST_TICK = new HashMap<>();

  public static void bootstrap() {
    // 安装默认 ReactionService 实现
    ReactionAPI.set(new DefaultReactionService());
    // 注册 tick 清理（经由 TickEngineHub）
    TickEngineHub.register(TickEngineHub.PRIORITY_REACTION, ReactionRegistry::handleServerTick);
    // 注册通用火系命中监听（非火衣的火系伤害也会刷新 Ignite）
    NeoForge.EVENT_BUS.addListener(
        net.tigereye.chestcavity.util.reaction.FireHitEvents::onIncomingDamage);
    // 注册默认规则：火衣 + 油涂层 => 爆炸 + 移除油 + 短暂屏蔽火衣结算
    registerDefaults();
    // 注册元素规则（冰/魂/腐蚀/联动）
    registerElementalDefaults();
    // 火系通用规则（Ignite DoT 刷新/过热判定等）
    registerFireGenericDefaults();
    registerDragonDefaults();
    // 注册血道规则（血印/失血 与 各系 DoT 的联动）
    registerBloodDefaults();
    // 注册光/魂/剑道联动规则
    registerLightSoulSwordDefaults();
    // 注册雷/木/人道联动规则
    registerLeiMuRenDefaults();
    registerShiShuiTianDefaults();
    registerTuXinYanYunDefaults();
    // 注册毒道/骨道等扩展规则
    registerToxicDefaults();
    registerBoneDefaults();
    registerZhiDaoDefaults();
  }

  private static void registerZhiDaoDefaults() {
    // 智慧裂脑：魂焰 × 智慧标记（wisdom_mark）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.WISDOM_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.MIND_IMMUNE),
        ctx -> {
          final int IMMUNE_SHORT = 40;
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          // 取消本次魂焰伤害，改为直伤 + 虚弱
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 3.0F);
          } else {
            target.hurt(target.damageSources().generic(), 3.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WISDOM_MARK);
          ReactionTagOps.add(target, ReactionTagKeys.MIND_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.mind_rupture.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.mind_rupture.target", a);
          return ReactionResult.cancel();
        });

    // 烧真：火衣 × 幻像
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.ILLUSION),
        ctx -> {
          if (ctx.target().level() instanceof ServerLevel level) {
            float r = 2.0F;
            double dmg = 2.0D;
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                ctx.target().getX(),
                ctx.target().getY(),
                ctx.target().getZ(),
                r,
                dmg,
                ctx.attacker());
          }
          ReactionTagOps.clear(ctx.target(), ReactionTagKeys.ILLUSION);
          String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "火衣";
          String t = ctx.target().getName().getString();
          i18nMessage(ctx.attacker(), "message.chestcavity.reaction.burn_false.attacker", t);
          i18nMessage(ctx.target(), "message.chestcavity.reaction.burn_false.target", a);
          return ReactionResult.proceed();
        });

    // 冻清：霜痕 × 困惑
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.CONFUSION)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
        ctx -> {
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 2.0F);
          } else {
            target.hurt(target.damageSources().generic(), 2.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.CONFUSION);
          ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, 40);
          String a = attacker != null ? attacker.getName().getString() : "霜";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.clarity_freeze.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.clarity_freeze.target", a);
          return ReactionResult.cancel();
        });

    // 专注净化：腐蚀 × 专注（落在自身时触发）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.FOCUS),
        ctx -> {
          final int IMMUNE_SHORT = 40;
          ReactionTagOps.add(ctx.target(), ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
          i18nMessage(ctx.target(), "message.chestcavity.reaction.focus_purify.self");
          return ReactionResult.cancel();
        });

    // 心灵灼烧加深：心理灼烧 × 智慧标记
    register(
        net.tigereye.chestcavity.util.DoTTypes.ZHI_DAO_PSYCHIC_BURN,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.WISDOM_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.MIND_IMMUNE),
        ctx -> {
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          // 额外直伤模拟加深
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 2.0F);
          } else {
            target.hurt(target.damageSources().generic(), 2.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WISDOM_MARK);
          ReactionTagOps.add(target, ReactionTagKeys.MIND_IMMUNE, 40);
          String a = attacker != null ? attacker.getName().getString() : "心念";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.psychic_burn_amplify.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.psychic_burn_amplify.target", a);
          return ReactionResult.proceed();
        });
  }

  // 火系通用：点燃刷新、过热爆、点燃期间加成等
  private static void registerFireGenericDefaults() {
    final CCConfig.ReactionConfig C0 = ChestCavity.config.REACTION;
    // 火衣/火系 DoT 命中：刷新 Ignite，并检查过热爆（char_pressure≥3）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx -> !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
        ctx -> {
          // 刷新 Ignite DoT
          scheduleOrRefreshIgnite(ctx.attacker(), ctx.target());
          // 过热爆：当热压达到3层
          if (ReactionTagOps.count(ctx.target(), ReactionTagKeys.CHAR_PRESSURE) >= 3) {
            float power = Math.max(0.8F, C0.HUO_TAN.overheatExplosionPower);
            if (ctx.target().level() instanceof ServerLevel level) {
              net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueExplosion(
                  level,
                  ctx.target().getX(),
                  ctx.target().getY(),
                  ctx.target().getZ(),
                  power,
                  ctx.attacker(),
                  false,
                  net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
            }
            ReactionTagOps.clear(ctx.target(), ReactionTagKeys.CHAR_PRESSURE);
            ReactionTagOps.add(
                ctx.target(),
                ReactionTagKeys.FIRE_IMMUNE,
                Math.max(0, C0.HUO_TAN.overheatFireImmuneTicks));
          }
          return ReactionResult.proceed();
        });

    // Ignite DoT 自身：若处于火免则取消；点燃期间追加少量真实/魔法伤害（护甲衰减近似修正）
    register(
        net.tigereye.chestcavity.util.DoTTypes.IGNITE,
        ctx -> true,
        ctx -> {
          if (ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE)) {
            return ReactionResult.cancel();
          }
          double bonus = Math.max(0.0D, C0.HUO_TAN.igniteBonusOnPulse);
          if (bonus > 0.0D) {
            LivingEntity attacker = ctx.attacker();
            LivingEntity target = ctx.target();
            if (attacker != null) {
              target.hurt(attacker.damageSources().magic(), (float) bonus);
            } else {
              target.hurt(target.damageSources().magic(), (float) bonus);
            }
          }
          return ReactionResult.proceed();
        });
  }

  public static void scheduleOrRefreshIgnite(LivingEntity attacker, LivingEntity target) {
    if (attacker == null || target == null) return;
    CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
    int stacks = ReactionTagOps.count(target, ReactionTagKeys.FLAME_MARK);
    double base = Math.max(0.0D, C.HUO_TAN.igniteBaseDps);
    double k = Math.max(0.0D, C.HUO_TAN.ignitePerStackK);
    int durTicks = Math.max(20, C.HUO_TAN.igniteDurationTicks);
    int seconds = Math.max(1, durTicks / 20);
    double dps = base + stacks * k;
    if (dps <= 0.0D) return;
    net.tigereye.chestcavity.engine.dot.DoTEngine.schedulePerSecond(
        attacker,
        target,
        dps,
        seconds,
        null,
        1.0f,
        1.0f,
        net.tigereye.chestcavity.util.DoTTypes.IGNITE,
        null,
        net.tigereye.chestcavity.engine.dot.DoTEngine.FxAnchor.TARGET,
        net.minecraft.world.phys.Vec3.ZERO,
        1.0f);
  }

  private static void registerTuXinYanYunDefaults() {
    final int IMMUNE_SHORT = 40;

    // 土墙庇护（腐蚀 × 土之护）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.EARTH_GUARD)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true));
          if (attacker != null) {
            attacker.addEffect(
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
          }
          ReactionTagOps.clear(target, ReactionTagKeys.EARTH_GUARD);
          ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "腐蚀";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.earth_guard_corrosion.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.earth_guard_corrosion.target", a);
          return ReactionResult.cancel();
        });

    // 土焰护墙（火衣 × 土之护）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.EARTH_GUARD)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 8 * 20, 1, false, true));
          if (attacker != null) {
            attacker.hurt(attacker.damageSources().indirectMagic(target, target), 2.0F);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.EARTH_GUARD);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.earth_guard_flame.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.earth_guard_flame.target", a);
          return ReactionResult.cancel();
        });

    // 石壳护寒（霜痕 × 石甲）
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.STONE_SHELL)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true));
          if (attacker != null) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
          }
          ReactionTagOps.clear(target, ReactionTagKeys.STONE_SHELL);
          ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "寒气";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.stone_shell_frost.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.stone_shell_frost.target", a);
          return ReactionResult.cancel();
        });

    // 石火反震（火衣 × 石甲）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.STONE_SHELL),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            attacker.hurt(attacker.damageSources().thorns(target), 3.0F);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.STONE_SHELL);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.stone_shell_flame.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.stone_shell_flame.target", a);
          return ReactionResult.cancel();
        });

    // 星辉护心（魂焰 × 星辉）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.STAR_GLINT),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          ReactionTagOps.clear(target, ReactionTagKeys.STAR_GLINT);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          target.heal(4.0F);
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.star_glint_soul.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.star_glint_soul.target", a);
          return ReactionResult.cancel();
        });

    // 云火蒸腾（火衣 × 云幕 + 火痕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.CLOUD_SHROUD)
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.FLAME_TRAIL),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                2.5F,
                2.5D,
                attacker,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.STEAM);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.CLOUD_SHROUD);
          ReactionTagOps.clear(target, ReactionTagKeys.FLAME_TRAIL);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.flame_trail_cloud.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.flame_trail_cloud.target", a);
          return ReactionResult.cancel();
        });
  }

  private static void registerToxicDefaults() {
    // 火衣 × 臭云（STENCH_CLOUD）=> 毒燃闪爆
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.STENCH_CLOUD)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.TOXIC_IMMUNE),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          if (ctx.target().level() instanceof ServerLevel level) {
            float r = Math.max(1.5F, 2.0F);
            double dmg = Math.max(2.0D, 3.5D);
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                ctx.target().getX(),
                ctx.target().getY(),
                ctx.target().getZ(),
                r,
                dmg,
                ctx.attacker(),
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.STENCH_CLOUD)
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
    final int IMMUNE_SHORT = 40;
    final int SHARD_IMMUNE = 80;

    // 骨蚀裂（腐蚀 × 骨印）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_IMMUNE),
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
          ReactionTagOps.add(target, ReactionTagKeys.BONE_IMMUNE, IMMUNE_SHORT);
          i18nMessage(
              attacker,
              "message.chestcavity.reaction.bone_corrosion_crack.attacker",
              target.getName().getString());
          i18nMessage(
              target,
              "message.chestcavity.reaction.bone_corrosion_crack.target",
              attacker != null ? attacker.getName().getString() : "腐蚀");
          return ReactionResult.proceed();
        });

    // 骨煅灼（火衣 × 骨印）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          float bonus = 4.0F;
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), bonus);
          } else {
            target.hurt(target.damageSources().generic(), bonus);
          }
          target.igniteForSeconds(3);
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                2.5F,
                3.0D,
                attacker,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.BONE_MARK);
          ReactionTagOps.add(target, ReactionTagKeys.BONE_IMMUNE, IMMUNE_SHORT);
          i18nMessage(
              attacker,
              "message.chestcavity.reaction.bone_calcine.attacker",
              target.getName().getString());
          i18nMessage(
              target,
              "message.chestcavity.reaction.bone_calcine.target",
              attacker != null ? attacker.getName().getString() : "火焰");
          return ReactionResult.proceed();
        });

    // 骨棘霜爆（霜痕 × 骨棘场）
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.SHARD_FIELD)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            target.hurt(attacker.damageSources().generic(), 3.0F);
          } else {
            target.hurt(target.damageSources().generic(), 3.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.ResidueManager.spawnOrRefreshFrost(
                level, target.getX(), target.getY(), target.getZ(), 2.5F, 120, 1);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.SHARD_FIELD);
          ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, SHARD_IMMUNE);
          ReactionTagOps.add(target, ReactionTagKeys.BONE_IMMUNE, IMMUNE_SHORT);
          i18nMessage(
              attacker,
              "message.chestcavity.reaction.bone_shard_frost.attacker",
              target.getName().getString());
          i18nMessage(
              target,
              "message.chestcavity.reaction.bone_shard_frost.target",
              attacker != null ? attacker.getName().getString() : "寒霜");
          return ReactionResult.proceed();
        });

    // 骨魂共鸣（魂焰 × 骨印）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BONE_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          DelayedDamageEngine.schedule(ctx.server(), attacker, target, 5.0D, 40);
          target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                2.0F,
                2.0D,
                attacker,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.SOUL);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.BONE_MARK);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          ReactionTagOps.add(target, ReactionTagKeys.BONE_IMMUNE, IMMUNE_SHORT);
          i18nMessage(
              attacker,
              "message.chestcavity.reaction.bone_soul_resonance.attacker",
              target.getName().getString());
          i18nMessage(
              target,
              "message.chestcavity.reaction.bone_soul_resonance.target",
              attacker != null ? attacker.getName().getString() : "魂焰");
          return ReactionResult.proceed();
        });
  }

  private static void registerDefaults() {
    // 规则：当触发 DoT 为 火衣光环，目标身上存在 OIL_COATING（切换至通用 Tag 判定）
    register(
        ReactionStatuses.OIL_COATING_TRIGGER_DOT,
        ctx ->
            net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(
                ctx.target(),
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
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                x,
                y,
                z,
                r,
                dmg,
                null,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
          } else {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueExplosion(
                level,
                x,
                y,
                z,
                power,
                null,
                true,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
          }
          ReactionEvents.fireOil(ctx);
          // 移除油涂层（通用 Tag 清理）
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(
              ctx.target(), net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.OIL_COATING);
          // 可选：移除火痕，避免后续被其它火源立刻再次连锁
          if (C.fireOilClearFlameTrail) {
            net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(
                ctx.target(),
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.FLAME_TRAIL);
          }
          // 为目标添加短暂火系免疫，进一步抑制立即连锁
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
              ctx.target(),
              net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.FIRE_IMMUNE,
              Math.max(0, C.fireOilImmuneTicks));
          // 为攻击者添加火衣屏蔽窗口（对所有目标生效），防止同一攻击者短时间内反复触发
          ReactionRuntime.blockFireOil(ctx.attacker(), Math.max(0, C.fireOilBlockTicks));
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            (ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
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
            target.hurt(
                attacker.damageSources().mobAttack(attacker), (float) Math.max(0.0D, bonus));
          } else {
            target.hurt(target.damageSources().generic(), (float) Math.max(0.0D, bonus));
          }
          // 清除血印，短暂火系免疫并赋燃幅提示
          ReactionTagOps.clear(target, ReactionTagKeys.BLOOD_MARK);
          ReactionTagOps.add(target, ReactionTagKeys.IGNITE_AMP, 60);
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.BLOOD_MARK)
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

  private static void registerLightSoulSwordDefaults() {
    final int IMMUNE_SHORT = 40;

    // 光魂共振（魂焰 × 光晕眩目）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.LIGHT_DAZE)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          float bonus = 4.0F;
          if (attacker != null) {
            target.hurt(attacker.damageSources().magic(), bonus);
          } else {
            target.hurt(target.damageSources().magic(), bonus);
          }
          target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true));
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                2.0F,
                2.0D,
                attacker,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.SOUL);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.LIGHT_DAZE);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.light_soul_burst.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.light_soul_burst.target", a);
          return ReactionResult.proceed();
        });

    // 剑魂裂击（魂焰 × 剑痕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.SWORD_SCAR)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          float bonus = 3.5F;
          if (attacker != null) {
            target.hurt(attacker.damageSources().magic(), bonus);
          } else {
            target.hurt(target.damageSources().magic(), bonus);
          }
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.SWORD_SCAR);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.soul_rend_blade.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.soul_rend_blade.target", a);
          return ReactionResult.proceed();
        });

    // 灵魂炽燃（火衣 × 灵痕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_SCAR)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          float bonus = 5.0F;
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), bonus);
          } else {
            target.hurt(target.damageSources().generic(), bonus);
          }
          target.igniteForSeconds(2);
          ReactionTagOps.clear(target, ReactionTagKeys.SOUL_SCAR);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.soul_pyre.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.soul_pyre.target", a);
          return ReactionResult.proceed();
        });
  }

  private static void registerLeiMuRenDefaults() {
    final int IMMUNE_SHORT = 40;

    // 雷霜锁链（霜痕 × 雷痕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.LIGHTNING_CHARGE)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.LIGHTNING_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            target.hurt(attacker.damageSources().magic(), 3.0F);
          } else {
            target.hurt(target.damageSources().magic(), 3.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 2, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.LIGHTNING_CHARGE);
          ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "霜痕";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.lightning_frost_chain.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.lightning_frost_chain.target", a);
          return ReactionResult.proceed();
        });

    // 雷炎爆轰（火衣 × 雷痕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.LIGHTNING_CHARGE)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.LIGHTNING_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), 4.0F);
          } else {
            target.hurt(target.damageSources().magic(), 4.0F);
          }
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                2.2F,
                3.5D,
                attacker,
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.LIGHTNING_CHARGE);
          ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.lightning_surge.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.lightning_surge.target", a);
          return ReactionResult.proceed();
        });

    // 雷枢感电：每跳强化迟缓并续雷痕
    register(
        net.tigereye.chestcavity.util.DoTTypes.LEI_DUN_ELECTRIFY,
        ctx -> true,
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 2, false, true));
          if (attacker != null
              && net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil.areEnemies(
                  attacker, target)) {
            ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, 60);
          } else {
            ReactionTagOps.add(target, ReactionTagKeys.LIGHTNING_CHARGE, 40);
          }
          return ReactionResult.proceed();
        });

    // 木灵护熄（火衣 × 木灵）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.WOOD_GROWTH),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.heal(3.0F);
          target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WOOD_GROWTH);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.wood_purify.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.wood_purify.target", a);
          return ReactionResult.cancel();
        });

    // 木灵护体（腐蚀 × 木灵）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.WOOD_GROWTH),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 8 * 20, 1, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WOOD_GROWTH);
          ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "腐蚀";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.wood_shield_bloom.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.wood_shield_bloom.target", a);
          return ReactionResult.cancel();
        });

    // 金刚护心（魂焰 × 人道守心）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.HUMAN_AEGIS),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          ReactionTagOps.clear(target, ReactionTagKeys.HUMAN_AEGIS);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          target.heal(2.0F);
          if (attacker != null && attacker.isAlive()) {
            attacker.hurt(attacker.damageSources().indirectMagic(target, target), 2.0F);
          }
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.human_aegis_soul.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.human_aegis_soul.target", a);
          return ReactionResult.cancel();
        });

    // 金刚护体（腐蚀 × 人道守心）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.HUMAN_AEGIS),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          ReactionTagOps.clear(target, ReactionTagKeys.HUMAN_AEGIS);
          ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
          String a = attacker != null ? attacker.getName().getString() : "腐蚀";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.human_aegis_corrosion.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.human_aegis_corrosion.target", a);
          return ReactionResult.cancel();
        });
  }

  private static void registerShiShuiTianDefaults() {
    final int IMMUNE_SHORT = 40;

    // 醉食魂震（魂焰 × 醉食）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.FOOD_MADNESS)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            target.hurt(attacker.damageSources().magic(), 5.0F);
          } else {
            target.hurt(target.damageSources().magic(), 5.0F);
          }
          target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.FOOD_MADNESS);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.food_madness_soul.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.food_madness_soul.target", a);
          return ReactionResult.proceed();
        });

    // 醉食炎爆（火衣 × 醉食）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.FOOD_MADNESS)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 4.0F);
          } else {
            target.hurt(target.damageSources().generic(), 4.0F);
          }
          target.igniteForSeconds(4);
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.FOOD_MADNESS);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.food_madness_flame.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.food_madness_flame.target", a);
          return ReactionResult.proceed();
        });

    // 水幕熄焰（火衣 × 水幕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.WATER_VEIL),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          target.heal(3.0F);
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WATER_VEIL);
          ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, IMMUNE_SHORT);
          String a = attacker != null ? attacker.getName().getString() : "火衣";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.water_veil_fire.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.water_veil_fire.target", a);
          return ReactionResult.cancel();
        });

    // 水霜回流（霜痕 × 水幕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.WATER_VEIL)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
        ctx -> {
          LivingEntity target = ctx.target();
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WATER_VEIL);
          ReactionTagOps.add(target, ReactionTagKeys.FROST_IMMUNE, IMMUNE_SHORT);
          String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "霜痕";
          String t = target.getName().getString();
          i18nMessage(ctx.attacker(), "message.chestcavity.reaction.water_veil_frost.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.water_veil_frost.target", a);
          return ReactionResult.cancel();
        });

    // 灵水护魂（魂焰 × 水幕）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.WATER_VEIL)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          LivingEntity target = ctx.target();
          target.heal(2.0F);
          target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
          ReactionTagOps.clear(target, ReactionTagKeys.WATER_VEIL);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(ctx.attacker(), "message.chestcavity.reaction.water_veil_soul.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.water_veil_soul.target", a);
          return ReactionResult.cancel();
        });

    // 天守护心（魂焰 × 天道护心）
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.HEAVEN_GRACE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          ReactionTagOps.clear(target, ReactionTagKeys.HEAVEN_GRACE);
          ReactionTagOps.add(target, ReactionTagKeys.SOUL_IMMUNE, IMMUNE_SHORT);
          target.heal(2.0F);
          if (attacker != null && attacker.isAlive()) {
            attacker.hurt(attacker.damageSources().indirectMagic(target, target), 2.0F);
          }
          String a = attacker != null ? attacker.getName().getString() : "魂焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.heaven_grace_soul.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.heaven_grace_soul.target", a);
          return ReactionResult.cancel();
        });

    // 天守护体（腐蚀 × 天道护心）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.HEAVEN_GRACE),
        ctx -> {
          LivingEntity attacker = ctx.attacker();
          LivingEntity target = ctx.target();
          ReactionTagOps.clear(target, ReactionTagKeys.HEAVEN_GRACE);
          ReactionTagOps.add(target, ReactionTagKeys.CORROSION_IMMUNE, IMMUNE_SHORT);
          target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true));
          target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
          String a = attacker != null ? attacker.getName().getString() : "腐蚀";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.heaven_grace_corrosion.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.heaven_grace_corrosion.target", a);
          return ReactionResult.cancel();
        });
  }

  private static void registerElementalDefaults() {
    // 霜痕碎裂（FROST_SHATTER）
    register(
        net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE,
        ctx ->
            ChestCavity.config.REACTION.enableFrost
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_IMMUNE),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          // 取消本次 DoT，执行一次直伤 + 击退
          if (attacker != null) {
            target.hurt(
                attacker.damageSources().mobAttack(attacker),
                (float) Math.max(0.0D, C.frostShatterDamage));
          } else {
            target.hurt(
                target.damageSources().generic(), (float) Math.max(0.0D, C.frostShatterDamage));
          }
          // 击退
          try {
            if (attacker != null) {
              var push =
                  target
                      .position()
                      .subtract(attacker.position())
                      .normalize()
                      .scale(Math.max(0.0D, C.frostShatterKnockback));
              target.push(push.x, 0.02D, push.z);
              target.hurtMarked = true;
            }
          } catch (Throwable ignored) {
          }
          // 残留概率生成（缓慢云）→ 交给 ReactionEngine 合并/刷新
          if (target.level() instanceof ServerLevel level) {
            if (level.getRandom().nextDouble()
                < Math.max(0.0D, Math.min(1.0D, C.frostResidueChance))) {
              net.tigereye.chestcavity.engine.reaction.ResidueManager.spawnOrRefreshFrost(
                  level,
                  target.getX(),
                  target.getY(),
                  target.getZ(),
                  C.frostResidueRadius,
                  C.frostResidueDurationTicks,
                  C.frostResidueSlowAmplifier);
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ChestCavity.config.REACTION.enableSteamScald
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.FROST_MARK),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          if (ctx.target().level() instanceof ServerLevel level) {
            float radius = Math.max(0.5F, C.steamScaldRadius);
            double dmg = Math.max(0.0D, C.steamScaldDamage);
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                level,
                ctx.target().getX(),
                ctx.target().getY(),
                ctx.target().getZ(),
                radius,
                dmg,
                ctx.attacker(),
                net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.STEAM);
            // 目标本身立刻附加失明，范围内其他实体的 Debuff 如需可在 Engine 侧扩展
            ctx.target()
                .addEffect(
                    new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        Math.max(0, C.steamScaldBlindnessTicks),
                        0,
                        false,
                        true));
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
    register(
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        ctx ->
            ChestCavity.config.REACTION.enableSoulEcho
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.SOUL_IMMUNE),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          scheduleDelayedDamage(
              ctx.server(),
              ctx.attacker(),
              ctx.target(),
              Math.max(0.0D, C.soulEchoDamage),
              Math.max(0, C.soulEchoDelayTicks));
          ReactionTagOps.clear(ctx.target(), ReactionTagKeys.SOUL_MARK);
          ReactionTagOps.add(
              ctx.target(), ReactionTagKeys.SOUL_IMMUNE, Math.max(0, C.soulImmuneTicks));
          {
            int dt = Math.max(0, C.soulEchoDelayTicks);
            String a = ctx.attacker() != null ? ctx.attacker().getName().getString() : "未知";
            String t = ctx.target().getName().getString();
            String secs = String.format(Locale.ROOT, "%.1f", dt / 20.0);
            i18nMessage(ctx.attacker(), "message.chestcavity.reaction.soul_echo.attacker", secs, t);
            i18nMessage(ctx.target(), "message.chestcavity.reaction.soul_echo.target", a);
          }
          return ReactionResult.proceed(); // 本次魂焰 DoT 照常结算
        });

    // 腐蚀激增
    register(
        net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
        ctx ->
            ChestCavity.config.REACTION.enableCorrosionSurge
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_IMMUNE),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          // 额外伤害 + 虚弱
          if (attacker != null) {
            target.hurt(
                attacker.damageSources().mobAttack(attacker),
                (float) Math.max(0.0D, C.corrosionSurgeBonusDamage));
          } else {
            target.hurt(
                target.damageSources().generic(),
                (float) Math.max(0.0D, C.corrosionSurgeBonusDamage));
          }
          target.addEffect(
              new MobEffectInstance(
                  MobEffects.WEAKNESS, Math.max(0, C.corrosionSurgeWeaknessTicks), 0, false, true));
          if (target.level() instanceof ServerLevel level) {
            net.tigereye.chestcavity.engine.reaction.ResidueManager.spawnOrRefreshCorrosion(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                C.corrosionResidueRadius,
                C.corrosionResidueDurationTicks);
          }
          ReactionTagOps.clear(target, ReactionTagKeys.CORROSION_MARK);
          ReactionTagOps.add(
              target, ReactionTagKeys.CORROSION_IMMUNE, Math.max(0, C.corrosionImmuneTicks));
          {
            String a = attacker != null ? attacker.getName().getString() : "环境";
            String t = target.getName().getString();
            i18nMessage(attacker, "message.chestcavity.reaction.corrosion_surge.attacker", t);
            i18nMessage(target, "message.chestcavity.reaction.corrosion_surge.target", a);
          }
          return ReactionResult.proceed();
        });

    // 火×腐蚀（小爆）
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_HUO_YI_AURA,
        ctx ->
            ChestCavity.config.REACTION.enableFireCorrosion
                && ReactionTagOps.has(ctx.target(), ReactionTagKeys.CORROSION_MARK)
                && !ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_IMMUNE),
        ctx -> {
          CCConfig.ReactionConfig C = ChestCavity.config.REACTION;
          if (ctx.target().level() instanceof ServerLevel level) {
            if (C.fireCorrosionUseAoE) {
              net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
                  level,
                  ctx.target().getX(),
                  ctx.target().getY(),
                  ctx.target().getZ(),
                  Math.max(0.5F, C.fireCorrosionAoERadius),
                  Math.max(0.0D, C.fireCorrosionAoEDamage),
                  ctx.attacker(),
                  net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
            } else {
              float power =
                  Math.max(0.1F, Math.min(C.fireCorrosionExplosionPower, C.fireCorrosionMaxPower));
              net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueExplosion(
                  level,
                  ctx.target().getX(),
                  ctx.target().getY(),
                  ctx.target().getZ(),
                  power,
                  ctx.attacker(),
                  false,
                  net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
            }
          }
          ReactionTagOps.clear(ctx.target(), ReactionTagKeys.CORROSION_MARK);
          // 添加短 CD 防抖，避免频繁引爆导致卡顿
          ReactionTagOps.add(
              ctx.target(), ReactionTagKeys.FIRE_IMMUNE, Math.max(0, C.fireCorrosionCooldownTicks));
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

  public static void register(
      ResourceLocation triggerDotType, ReactionPredicate predicate, ReactionAction action) {
    RULES
        .computeIfAbsent(triggerDotType, k -> new ArrayList<>())
        .add(new ReactionRule(predicate, action));
  }

  // 在 DoT 伤害应用前调用；若返回 false，跳过本次伤害
  public static boolean preApplyDoT(
      MinecraftServer server,
      ResourceLocation dotTypeId,
      LivingEntity attacker,
      LivingEntity target) {
    if (server == null || dotTypeId == null || attacker == null || target == null) {
      return true;
    }
    long now = server.getTickCount();
    // 火衣屏蔽：若当前为火衣 DoT 且处于屏蔽窗口，则跳过
    if (ReactionStatuses.isHuoYiDot(dotTypeId) && ReactionRuntime.isFireOilBlocked(attacker, now))
      return false;
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

  public static void handleServerTick(ServerTickEvent.Post event) {
    long now = event.getServer().getTickCount();
    // 清理过期状态（委托 TagOps）
    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.purge(now);
    // 清理运行时屏蔽窗口 & 处理延迟伤害
    ReactionRuntime.purge(now);
    DelayedDamageEngine.process(event.getServer(), now);
    // 执行队列中的 AoE/爆炸 与 残留逻辑
    EffectsEngine.process(event.getServer());
    ResidueManager.tickFireResidues(event.getServer());
    if (!DRAGON_FLAME_IGNITE_LAST_TICK.isEmpty()) {
      long cutoff = now - 200L;
      DRAGON_FLAME_IGNITE_LAST_TICK.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }
  }

  private static void registerDragonDefaults() {
    final int DRAGON_FLAME_DURATION_TICKS = 6 * 20;
    final int DRAGON_FLAME_MAX_STACKS = 6;
    final int DRAGON_FLAME_IGNITE_EXTENSION_TICKS = 20;
    final int DRAGON_FLAME_IGNITE_GATE_TICKS = 40;

    // 劫火引爆：龙焰印记 + 油涂层
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_DRAGONFLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.OIL_COATING)
                && ReactionTagOps.count(ctx.target(), ReactionTagKeys.DRAGON_FLAME_MARK) > 0,
        ctx -> {
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          int stacks = Math.max(1, ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK));
          ReactionTagOps.clear(target, ReactionTagKeys.OIL_COATING);
          float damage = stacks * 10.0F;
          if (attacker != null) {
            target.hurt(attacker.damageSources().mobAttack(attacker), damage);
          } else {
            target.hurt(target.damageSources().generic(), damage);
          }
          if (target.level() instanceof ServerLevel level) {
            EffectsEngine.queueExplosion(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                0.6F + stacks * 0.05F,
                attacker,
                false,
                EffectsEngine.VisualTheme.FIRE);
            HuoLongFx.playReactionOil(target, stacks);
          }
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 0, false, true));
          String a = attacker != null ? attacker.getName().getString() : "龙焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.dragon_flame_oil.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.dragon_flame_oil.target", a);
          return ReactionResult.proceed();
        });

    // 龙火洗礼：龙焰印记 + 火衣光环
    register(
        net.tigereye.chestcavity.util.DoTTypes.YAN_DAO_DRAGONFLAME,
        ctx ->
            ReactionTagOps.has(ctx.target(), ReactionTagKeys.FIRE_COAT)
                && ReactionTagOps.count(ctx.target(), ReactionTagKeys.DRAGON_FLAME_MARK) > 0,
        ctx -> {
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          int current = ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK);
          int desired = Math.min(DRAGON_FLAME_MAX_STACKS, current + 2);
          int delta = desired - current;
          ReactionTagOps.addStacked(
              target, ReactionTagKeys.DRAGON_FLAME_MARK, delta, DRAGON_FLAME_DURATION_TICKS);
          ReactionTagOps.clear(target, ReactionTagKeys.FIRE_COAT);
          if (attacker != null) {
            ReactionTagOps.add(attacker, ReactionTagKeys.FIRE_IMMUNE, 40);
          }
          if (target.level() instanceof ServerLevel level && attacker != null) {
            EffectsEngine.queueAoEDamage(
                level, target.getX(), target.getY(), target.getZ(), 2.5F, 4.0D + desired, attacker);
            HuoLongFx.playReactionFireCoat(target, attacker, desired);
          }
          String a = attacker != null ? attacker.getName().getString() : "龙焰";
          String t = target.getName().getString();
          i18nMessage(attacker, "message.chestcavity.reaction.dragon_flame_fire_coat.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.dragon_flame_fire_coat.target", a);
          return ReactionResult.proceed();
        });

    // ignite Tick 延长龙焰持续
    register(
        net.tigereye.chestcavity.util.DoTTypes.IGNITE,
        ctx -> ReactionTagOps.has(ctx.target(), ReactionTagKeys.DRAGON_FLAME_MARK),
        ctx -> {
          LivingEntity target = ctx.target();
          LivingEntity attacker = ctx.attacker();
          UUID targetId = target.getUUID();
          long now = ctx.server().getTickCount();
          long last = DRAGON_FLAME_IGNITE_LAST_TICK.getOrDefault(targetId, Long.MIN_VALUE);
          if (now - last < DRAGON_FLAME_IGNITE_GATE_TICKS) {
            return ReactionResult.proceed();
          }
          int stacks = ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK);
          if (stacks <= 0) {
            return ReactionResult.proceed();
          }
          int remaining = ReactionTagOps.remainingTicks(target, ReactionTagKeys.DRAGON_FLAME_MARK);
          if (remaining <= 0) {
            return ReactionResult.proceed();
          }
          DRAGON_FLAME_IGNITE_LAST_TICK.put(targetId, now);
          int extended =
              Math.min(
                  remaining + DRAGON_FLAME_IGNITE_EXTENSION_TICKS,
                  DRAGON_FLAME_DURATION_TICKS + DRAGON_FLAME_IGNITE_EXTENSION_TICKS);
          ReactionTagOps.addStacked(target, ReactionTagKeys.DRAGON_FLAME_MARK, 0, extended);
          String a = attacker != null ? attacker.getName().getString() : "点燃";
          String t = target.getName().getString();
          i18nMessage(
              attacker, "message.chestcavity.reaction.dragon_flame_ignite_extend.attacker", t);
          i18nMessage(target, "message.chestcavity.reaction.dragon_flame_ignite_extend.target", a);
          return ReactionResult.proceed();
        });
  }

  // -------- 状态 API（过渡：统一走 TagOps） --------

  public static void addStatus(LivingEntity entity, ResourceLocation statusId, int durationTicks) {
    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
        entity,
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId),
        durationTicks);
  }

  public static boolean hasStatus(LivingEntity entity, ResourceLocation statusId) {
    return net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(
        entity, net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId));
  }

  public static void clearStatus(LivingEntity entity, ResourceLocation statusId) {
    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.clear(
        entity, net.tigereye.chestcavity.util.reaction.tag.ReactionTagAliases.resolve(statusId));
  }

  /** 指定攻击者在给定时长内禁止触发火衣-油涂层反应（委托引擎运行时）。 */
  public static void blockFireOil(LivingEntity attacker, int durationTicks) {
    ReactionRuntime.blockFireOil(attacker, durationTicks);
  }

  /** 当前是否处于火衣-油涂层反应的屏蔽窗口内（委托引擎运行时）。 */
  public static boolean isFireOilBlocked(LivingEntity attacker) {
    return ReactionRuntime.isFireOilBlocked(attacker);
  }

  // -------- 内部类型 --------

  public record ReactionContext(
      MinecraftServer server,
      ResourceLocation dotTypeId,
      LivingEntity attacker,
      LivingEntity target) {}

  public record ReactionResult(boolean cancelDamage) {
    public static ReactionResult proceed() {
      return new ReactionResult(false);
    }

    public static ReactionResult cancel() {
      return new ReactionResult(true);
    }
  }

  public record ReactionRule(ReactionPredicate predicate, ReactionAction action) {}

  @FunctionalInterface
  public interface ReactionPredicate {
    boolean test(ReactionContext ctx);
  }

  @FunctionalInterface
  public interface ReactionAction {
    ReactionResult apply(ReactionContext ctx);
  }

  // -------- Delayed damage (for Soul Echo) --------
  private static final List<DelayedDamage> DELAYED = new ArrayList<>();

  private record DelayedDamage(int dueTick, UUID attacker, UUID target, float amount) {}

  private static void scheduleDelayedDamage(
      MinecraftServer server,
      LivingEntity attacker,
      LivingEntity target,
      double amount,
      int delayTicks) {
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
  private static void spawnResidueFrost(
      ServerLevel level,
      double x,
      double y,
      double z,
      float radius,
      int durationTicks,
      int slowAmplifier) {
    try {
      AreaEffectCloud aec = new AreaEffectCloud(level, x, y, z);
      aec.setRadius(Math.max(0.5F, radius));
      aec.setDuration(Math.max(20, durationTicks));
      aec.setWaitTime(0);
      aec.setRadiusPerTick(0.0F);
      aec.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, slowAmplifier), false, true));
      level.addFreshEntity(aec);
    } catch (Throwable ignored) {
    }
  }

  private static void spawnResidueCorrosion(
      ServerLevel level, double x, double y, double z, float radius, int durationTicks) {
    try {
      AreaEffectCloud aec = new AreaEffectCloud(level, x, y, z);
      aec.setRadius(Math.max(0.5F, radius));
      aec.setDuration(Math.max(20, durationTicks));
      aec.setWaitTime(0);
      aec.setRadiusPerTick(0.0F);
      aec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
      level.addFreshEntity(aec);
    } catch (Throwable ignored) {
    }
  }

  private static void debugMessage(LivingEntity viewer, String text) {
    // 受配置 REACTION.debugReactions 控制
    CCConfig cfg = ChestCavity.config;
    if (cfg != null && !cfg.REACTION.debugReactions) return;
    if (viewer instanceof Player player && !player.level().isClientSide()) {
      player.sendSystemMessage(Component.literal(text));
    }
  }

  // 使用本地化键发送系统消息（优先用于调试/提示文案），以便与 guzhenren 风格保持一致
  private static void i18nMessage(LivingEntity viewer, String key, Object... args) {
    // 受配置 REACTION.debugReactions 控制（用于屏蔽反应调试/提示输出）
    CCConfig cfg = ChestCavity.config;
    if (cfg != null && !cfg.REACTION.debugReactions) return;
    if (viewer instanceof Player player && !player.level().isClientSide()) {
      player.sendSystemMessage(Component.translatable(key, args));
    }
  }
}
