package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainMath;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianYuGuDomainFX;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianYuGuDomainFXTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianYuGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYuGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 剑域蛊（独立器官）：
 * - 主动：一念开阖（打开6s调域窗口；结束后进入持续；开始即进入冷却）
 * - 被动：每2s按面积+收缩度扣费；主动期间改为每秒主动扣费
 * - 期间效果：正面锥额外-10%实伤
 */
public enum JianYuGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  public static final net.minecraft.resources.ResourceLocation ORGAN_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianyugu");
  public static final net.minecraft.resources.ResourceLocation ABILITY_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_yu_gu_adjust");

  // OrganState
  private static final String STATE_ROOT = "JianYuGu";
  private static final String K_TUNING_UNTIL = "TuningUntil"; // long tick
  private static final String K_ACTIVE_UNTIL = "ActiveUntil"; // long tick
  private static final String K_LAST_PASSIVE_DRAIN = "LastPassiveDrainAt"; // long tick
  private static final String K_LAST_ACTIVE_DRAIN = "LastActiveDrainAt"; // long tick
  private static final String K_READY_AT = "ReadyAt"; // long tick
  private static final String K_LAST_RADIUS = "LastRadius"; // double - 用于检测半径变化

  static {
    OrganActivationListeners.register(ABILITY_ID, JianYuGuOrganBehavior::activateAbility);
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) return;
    ItemStack organ = findMatchingOrgan(cc);
    OrganState st = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cd = MultiCooldown.builder(st).withSync(cc, organ).build();
    long now = player.serverLevel().getGameTime();

    long readyAt = st.getLong(K_READY_AT, 0L);
    if (now < readyAt) {
      JianYuGuFx.scheduleCooldownToast(player, ABILITY_ID, readyAt, now);
      return;
    }

    long tuningUntil = st.getLong(K_TUNING_UNTIL, 0L);
    long activeUntil = st.getLong(K_ACTIVE_UNTIL, 0L);

    // 若处于调域窗口：提前收束 → 进入持续
    if (now < tuningUntil) {
      enterActivePhase(player, st, now);
      return;
    }
    // 若处于持续：再次施放提前结束
    if (now < activeUntil) {
      st.setLong(K_ACTIVE_UNTIL, now);
      return;
    }

    // 启动调域窗口：支付开域成本；开始冷却；弹出 TUI
    if (!ResourceOps.payCost(player, JianYuGuTuning.OPEN_COST, "资源不足，无法开启剑域调节")) {
      return;
    }
    st.setLong(K_TUNING_UNTIL, now + JianYuGuTuning.TUNING_WINDOW_T);
    long cdReady = now + secondsToTicks(clampCooldownSeconds(0));
    st.setLong(K_READY_AT, cdReady);
    JianYuGuFx.scheduleCooldownToast(player, ABILITY_ID, cdReady, now);
    net.tigereye.chestcavity.compat.guzhenren.commands.SwordDomainConfigCommand.openTuiFor(
        player);

    // FX: 调域窗口开启特效
    double radius = net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
        .JianYuGuCalc.currentRadius(player);
    Vec3 center = player.position();
    JianYuGuDomainFX.spawnTuningStartEffect(player.serverLevel(), center, radius);
    st.setDouble(K_LAST_RADIUS, radius); // 记录初始半径
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) return;
    OrganState st = OrganState.of(organ, STATE_ROOT);
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();

    long tuningUntil = st.getLong(K_TUNING_UNTIL, 0L);
    long activeUntil = st.getLong(K_ACTIVE_UNTIL, 0L);
    boolean tuning = now < tuningUntil;
    boolean active = now < activeUntil;

    // 调域窗口结束→进入持续
    if (!tuning && !active && tuningUntil > 0L && now >= tuningUntil) {
      enterActivePhase(player, st, now);
      active = true;
    }

    // FX: 渲染领域特效
    if (tuning || active) {
      double radius = net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
          .JianYuGuCalc.currentRadius(player);
      Vec3 center = player.position();

      // 检测半径变化
      double lastRadius = st.getDouble(K_LAST_RADIUS, radius);
      if (Math.abs(radius - lastRadius) > 0.1) {
        // 半径变化特效
        JianYuGuDomainFX.spawnRadiusChangeEffect(level, center, lastRadius, radius);
        st.setDouble(K_LAST_RADIUS, radius);
      }

      boolean isSmallDomain = radius <= JianYuGuDomainFXTuning.SMALL_DOMAIN_RADIUS_THRESHOLD;

      if (tuning) {
        // 调域窗口特效
        JianYuGuDomainFX.spawnTuningBorderPulse(level, center, radius, now);
        JianYuGuDomainFX.spawnTuningCenterFocus(level, center, now);
        JianYuGuDomainFX.spawnRadiusMarkers(level, center, radius, now);
      } else if (active) {
        // 主动期间特效
        JianYuGuDomainFX.spawnActiveBorderAura(level, center, radius, now, isSmallDomain);

        // 小域模式特殊效果
        if (isSmallDomain) {
          JianYuGuDomainFX.spawnSmallDomainFocus(level, center, now);
        }

        // 正面锥格挡提示
        JianYuGuDomainFX.spawnFrontConeIndicator(level, player, now);
      }
    }

    // 主动扣费（每秒）
    if (active) {
      long last = st.getLong(K_LAST_ACTIVE_DRAIN, 0L);
      if (now - last >= 20L) {
        st.setLong(K_LAST_ACTIVE_DRAIN, now);
        double perSec =
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
                .JianYuGuCalc.activeDrainPerSec(player);
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
            .tryAdjustZhenyuan(player, -perSec, true);
      }
      return; // 主动期间暂停被动扣费
    }

    // 被动扣费（每2秒）
    long last = st.getLong(K_LAST_PASSIVE_DRAIN, 0L);
    if (now - last >= 40L) {
      st.setLong(K_LAST_PASSIVE_DRAIN, now);
      double amt =
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
              .JianYuGuCalc.passiveDrainPer2s(player);
      net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
          .tryAdjustZhenyuan(player, -amt, true);
    }
  }

  // 期间：正面锥额外 -10% 实伤
  @Override
  public float onIncomingDamage(
      DamageSource source, LivingEntity owner, ChestCavityInstance cc, ItemStack organ, float dmg) {
    if (!(owner instanceof ServerPlayer player) || owner.level().isClientSide()) return dmg;
    OrganState st = OrganState.of(organ, STATE_ROOT);
    long now = player.serverLevel().getGameTime();
    if (now >= st.getLong(K_ACTIVE_UNTIL, 0L)) return dmg;
    var attacker = source == null ? null : source.getEntity();
    if (attacker instanceof LivingEntity living
        && net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
            .JianYuGuCalc.isInFrontCone(owner, living)) {
      double mult =
          net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
              .JianYuGuCalc.frontConeDamageMultiplier();
      dmg *= (float) mult;

      // FX: 格挡成功特效
      JianYuGuDomainFX.spawnBlockSuccessEffect(player.serverLevel(), player);
    }
    return dmg;
  }

  // ----- 内部：阶段转换与扣费 -----
  private static void enterActivePhase(ServerPlayer player, OrganState st, long now) {
    int layers = 0; // TODO: 接入层数系统
    double secs = clampActiveSeconds(layers);
    st.setLong(K_ACTIVE_UNTIL, now + secondsToTicks(secs));
    st.setLong(K_TUNING_UNTIL, 0L);
    st.setLong(K_LAST_ACTIVE_DRAIN, now);

    // FX: 主动期间开始特效
    double radius = net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator
        .JianYuGuCalc.currentRadius(player);
    Vec3 center = player.position();
    JianYuGuDomainFX.spawnActiveStartEffect(player.serverLevel(), center, radius);
  }

  // 计算逻辑已抽至 JianYuGuCalc（运算 -> Calc）

  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) return ItemStack.EMPTY;
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id != null && id.equals(ORGAN_ID)) return s;
    }
    return ItemStack.EMPTY;
  }

  private static int secondsToTicks(double s) {
    return (int) Math.round(s * 20.0);
  }

  private static double clampActiveSeconds(int layers) {
    double s = JianYuGuTuning.ACTIVE_BASE_S + JianYuGuTuning.ACTIVE_PER_LAYER_S * Math.max(0, layers);
    return Math.min(JianYuGuTuning.ACTIVE_MAX_S, s);
  }

  private static double clampCooldownSeconds(int layers) {
    double s = JianYuGuTuning.COOLDOWN_BASE_S + JianYuGuTuning.COOLDOWN_PER_LAYER_S * Math.max(0, layers);
    return Math.max(JianYuGuTuning.COOLDOWN_MIN_S, s);
  }
}
