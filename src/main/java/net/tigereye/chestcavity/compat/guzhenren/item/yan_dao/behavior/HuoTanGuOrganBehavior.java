package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.engine.reaction.EffectsEngine;
import net.tigereye.chestcavity.engine.reaction.ResidueManager;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/**
 * 单窍·火炭蛊（Yan Dao） - 使用通用 Reaction Tag：FIRE_MARK/IGNITE_WINDOW/FIRE_IMMUNE/IGNITE_AMP/CHAR_PRESSURE
 * - 简化版逻辑：命中叠燃痕，达阈值触发小型“炭爆”，清层并赋短免；产生少量蓄压（CHAR_PRESSURE） - 参数从 GUZHENREN_BEHAVIOR 的动态表读取（默认值内置）
 */
public final class HuoTanGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganOnHitListener {

  public static final HuoTanGuOrganBehavior INSTANCE = new HuoTanGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "dan_qiao_huo_tan_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID; // 4转极限技（主动）：炭核风暴

  private static final String STATE_ROOT = "HuoTanGu";
  private static final String KEY_TIER = "Tier"; // 1~4，默认1
  private static final String KEY_EXTREME_READY_TICK = "ExtremeReadyAt";

  private HuoTanGuOrganBehavior() {}

  static {
    OrganActivationListeners.register(ABILITY_ID, HuoTanGuOrganBehavior::activateExtreme);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
      return damage;
    }
    if (target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
      return damage;
    }
    if (source == null || source.getEntity() != attacker) {
      return damage;
    }

    // 读取参数（支持配置覆写）
    var HC = net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN;
    int markDuration = Math.max(1, HC.flameMarkDurationTicks);
    int igniteWindow = Math.max(1, HC.igniteWindowTicks);
    int fireImmuneTicks = 40;
    int pressureTTL = 200;
    float burstPowerBase = 1.0F;
    int igniteAmpTicks = 10;

    OrganState state = OrganState.of(organ, STATE_ROOT);
    int tier = Math.max(1, Math.min(4, state.getInt(KEY_TIER, 1)));
    int threshold =
        tier <= 1
            ? BehaviorConfigAccess.getInt(HuoTanGuOrganBehavior.class, "burstThresholdTier1", 2)
            : BehaviorConfigAccess.getInt(HuoTanGuOrganBehavior.class, "burstThresholdHigh", 3);

    // 叠加“燃痕”并刷新点燃窗口
    ReactionTagOps.addStacked(target, ReactionTagKeys.FLAME_MARK, 1, markDuration);
    ReactionTagOps.add(target, ReactionTagKeys.IGNITE_WINDOW, igniteWindow);

    // 2转：ignite_window 下额外 +1 层（每秒最多一次）
    if (tier >= 2
        && ReactionTagOps.has(target, ReactionTagKeys.IGNITE_WINDOW)
        && !ReactionTagOps.has(target, IGNITE_BONUS_CD)) {
      ReactionTagOps.addStacked(target, ReactionTagKeys.FLAME_MARK, 1, markDuration);
      ReactionTagOps.add(target, IGNITE_BONUS_CD, 20);
    }

    int stacks = ReactionTagOps.count(target, ReactionTagKeys.FLAME_MARK);
    if (stacks >= threshold) {
      // 触发“炭爆”：小功率爆炸/或 AoE（交由 EffectsEngine 做限流与VFX）
      if (target.level() instanceof ServerLevel level) {
        float power = burstPowerBase;
        net.tigereye.chestcavity.engine.reaction.EffectsEngine.queueAoEDamage(
            level,
            target.getX(),
            target.getY(),
            target.getZ(),
            Math.max(0.8F, power * 1.6F),
            Math.max(0.5D, power * 2.0D),
            player,
            net.tigereye.chestcavity.engine.reaction.EffectsEngine.VisualTheme.FIRE);
      }
      // 清层 + 赋免 + 蓄压 + 攻击者短时燃幅
      ReactionTagOps.clear(target, ReactionTagKeys.FLAME_MARK);
      ReactionTagOps.add(target, ReactionTagKeys.FIRE_IMMUNE, fireImmuneTicks);
      ReactionTagOps.addStacked(target, ReactionTagKeys.CHAR_PRESSURE, 1, pressureTTL);
      int cp = ReactionTagOps.count(target, ReactionTagKeys.CHAR_PRESSURE);
      if (cp >= 3) {
        i18n(attacker, "message.chestcavity.huo_tan.overheat_ready.attacker");
        i18n(target, "message.chestcavity.huo_tan.overheat_ready.target");
      }
      ReactionTagOps.add(player, ReactionTagKeys.IGNITE_AMP, igniteAmpTicks);
      // 链爆就绪提示：附近火雾≥2
      if (player.level() instanceof ServerLevel sl) {
        int count =
            net.tigereye.chestcavity.engine.reaction.ResidueManager.countFireResiduesNear(
                sl, player.getX(), player.getY(), player.getZ(), 5.0);
        if (count >= 2) {
          i18n(player, "message.chestcavity.huo_tan.chain_ready");
        }
      }

      // 提示（系统消息，便于验证）
      i18n(
          attacker,
          "message.chestcavity.reaction.charcoal_pop.attacker",
          target.getName().getString());
      i18n(
          target,
          "message.chestcavity.reaction.charcoal_pop.target",
          attacker.getName().getString());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[compat/guzhenren][yan_dao][huo_tan] onHit: tier={} stacks={} threshold={} target=\"{}\"",
          tier,
          stacks,
          threshold,
          target.getName().getString());
    }

    // 1转：5% 几率在命中点落下余烬（3s）；2转延至5s
    if (attacker.level() instanceof ServerLevel slevel) {
      if (attacker.getRandom().nextDouble() < 0.05D) {
        int dur = tier == 1 ? 60 : (tier >= 2 ? 100 : HC.emberResidueDurationTicks);
        float r = Math.max(0.5F, HC.emberResidueRadius);
        ResidueManager.spawnOrRefreshFire(
            slevel, target.getX(), target.getY(), target.getZ(), r, dur);
      }
    }

    // 4转被动链爆：附近≥2个火雾且自身有 ignite_amp，触发 3 段链爆（-20%/跳），并进入冷却
    if (tier >= 4
        && attacker instanceof Player
        && attacker.level() instanceof ServerLevel slevel2) {
      if (net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(
              player, net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.IGNITE_AMP)
          && !net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.has(
              player, net.tigereye.chestcavity.ChestCavity.id("reaction/chain_blast_cd"))) {
        int count =
            net.tigereye.chestcavity.engine.reaction.ResidueManager.countFireResiduesNear(
                slevel2, player.getX(), player.getY(), player.getZ(), 5.0);
        if (count >= 2) {
          var HC2 = net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN;
          int jumps = Math.max(1, HC2.chainBlastJumps);
          double dmg = Math.max(0.1D, HC2.chainBlastFirstDamage);
          float radius = Math.max(0.5F, HC2.chainBlastRadius);
          double decay = Math.max(0.1D, Math.min(1.0D, HC2.chainBlastDecay));
          i18n(player, "message.chestcavity.huo_tan.chain_triggered");
          for (int j = 0; j < jumps; j++) {
            final double jdmg = dmg;
            final float rr = radius;
            net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps.schedule(
                slevel2,
                () ->
                    EffectsEngine.queueAoEDamage(
                        slevel2,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        rr,
                        jdmg,
                        player,
                        EffectsEngine.VisualTheme.FIRE),
                j * 4);
            dmg *= decay;
          }
          int cd = Math.max(20, HC2.chainBlastCooldownTicks);
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
              player, net.tigereye.chestcavity.ChestCavity.id("reaction/chain_blast_cd"), cd);
        }
      }
    }
    return damage;
  }

  private static final ResourceLocation IGNITE_BONUS_CD =
      net.tigereye.chestcavity.ChestCavity.id("reaction/ignite_bonus_cd");

  // ===== 4转极限技：炭核风暴 =====
  private static final int EXTREME_SEGMENT_TICKS = 8; // 0.4s 固定段间
  private static final int EXTREME_SEGMENTS = 3;

  // 其余参数统一从 ReactionConfig.HUO_TAN 读取

  private static void activateExtreme(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = OrganState.of(organ, STATE_ROOT);
    int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, 4);
    if (tier < 4) {
      // 仅四转可用
      return;
    }
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry ready = cooldown.entry(KEY_EXTREME_READY_TICK);
    long now = player.level().getGameTime();
    if (ready.getReadyTick() > now) {
      return; // 冷却中
    }

    var HC = net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN;
    var payment =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.consumeStrict(
            player, HC.extremeZhenyuanCost, HC.extremeJingliCost);
    if (!payment.succeeded()) {
      return;
    }

    if (!(player.level() instanceof ServerLevel server)) {
      return;
    }

    // 收集范围目标与其 char_pressure 层数，随后清空并赋免疫窗
    Vec3 center = player.position();
    double ER = Math.max(0.5D, HC.extremeRadius);
    AABB box =
        new AABB(
            center.x - ER,
            center.y - ER,
            center.z - ER,
            center.x + ER,
            center.y + ER,
            center.z + ER);
    java.util.List<LivingEntity> targets =
        server.getEntitiesOfClass(
            LivingEntity.class,
            box,
            t ->
                t != null
                    && t.isAlive()
                    && t != player
                    && !t.isAlliedTo(player)
                    && t.distanceToSqr(center) <= ER * ER);

    java.util.Map<java.util.UUID, Float> damages = new java.util.HashMap<>();
    for (LivingEntity t : targets) {
      int stacks = ReactionTagOps.count(t, ReactionTagKeys.CHAR_PRESSURE);
      if (stacks <= 0) continue;
      ReactionTagOps.clear(t, ReactionTagKeys.CHAR_PRESSURE);
      ReactionTagOps.add(t, ReactionTagKeys.FIRE_IMMUNE, Math.max(20, HC.extremeFireImmuneTicks));
      float total = Math.max(0.0f, stacks * HC.extremeDmgPerPressure);
      damages.put(t.getUUID(), total);
    }

    // 分段伤害系数（40%/35%/25%）
    final float[] phases = new float[] {0.40f, 0.35f, 0.25f};
    for (int i = 0; i < EXTREME_SEGMENTS; i++) {
      final int phase = i;
      TickOps.schedule(
          server,
          () -> applyStormPhase(server, player, damages, phases[phase]),
          EXTREME_SEGMENT_TICKS * i);
    }

    long readyAt = now + Math.max(20, HC.extremeCooldownTicks);
    ready.setReadyAt(readyAt);
    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }
  }

  private static void applyStormPhase(
      ServerLevel level,
      Player caster,
      java.util.Map<java.util.UUID, Float> damages,
      float factor) {
    if (level == null || caster == null || damages == null || damages.isEmpty()) return;
    for (java.util.Map.Entry<java.util.UUID, Float> e : damages.entrySet()) {
      LivingEntity target = findLiving(level, e.getKey());
      if (target == null || !target.isAlive()) continue;
      float amount = e.getValue() * Math.max(0.0f, factor);
      if (amount <= 0.0f) continue;
      target.hurt(caster.damageSources().indirectMagic(caster, caster), amount);
    }
    // 可在此处追加粒子/音效反馈（避免重复伤害，不用引擎爆炸）。
  }

  private static LivingEntity findLiving(ServerLevel level, java.util.UUID id) {
    if (level == null || id == null) return null;
    net.minecraft.world.entity.Entity e = level.getEntity(id);
    return (e instanceof LivingEntity le) ? le : null;
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(v -> Math.max(0L, v), 0L);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private void i18n(LivingEntity viewer, String key, Object... args) {
    if (viewer instanceof Player p && !p.level().isClientSide()) {
      p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(key, args));
    }
  }
}
