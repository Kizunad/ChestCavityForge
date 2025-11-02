package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianXinGuStateOps;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianXinGuPassiveTuning;

/**
 * “定心返本”状态机的纯逻辑运算。
 */
public final class JianXinGuPassiveCalc {
  private JianXinGuPassiveCalc() {}

  // 与行为类保持一致的键（行为中已使用）
  public static final String K_FOCUS_METER = "FocusMeter"; // int [0,100]
  public static final String K_FOCUS_LOCK_UNTIL = "FocusLockUntil"; // long tick
  public static final String K_SWORD_MOMENTUM = "SwordMomentum"; // int [0,5]
  public static final String K_MEDITATING = "Meditating"; // boolean
  public static final String K_LAST_FOCUS_GAIN_AT = "FocusLastGainAt"; // long tick

  /** 是否属于“控制类效果”——由配置列表决定。 */
  public static boolean isControlEffect(MobEffectInstance inst) {
    if (inst == null) return false;
    net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder = inst.getEffect();
    ResourceLocation id =
        holder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
    if (id == null) return false;
    for (String s : JianXinGuPassiveTuning.NEGATIVE_EFFECT_IDS) {
      if (Objects.equals(id, ResourceLocation.parse(s))) {
        return true;
      }
    }
    return false;
  }

  /** 清除配置里的负面效果。*/
  public static void clearNegativeEffects(ServerPlayer player) {
    if (player == null) return;
    for (String s : JianXinGuPassiveTuning.NEGATIVE_EFFECT_IDS) {
      ResourceLocation id = ResourceLocation.parse(s);
      java.util.Optional<net.minecraft.core.Holder.Reference<MobEffect>> ref =
          BuiltInRegistries.MOB_EFFECT.getHolder(id);
      ref.ifPresent(player::removeEffect);
    }
  }

  /** 处理一次“受到控制效果”的累积。*/
  public static void onControlEffect(ServerPlayer player, OrganState state) {
    if (player == null || state == null) return;
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    long lockUntil = state.getLong(K_FOCUS_LOCK_UNTIL, 0L);
    if (now < lockUntil) {
      return; // 锁定期间不再积累
    }

    int cur = state.getInt(K_FOCUS_METER, 0);
    int next = Math.min(JianXinGuPassiveTuning.FOCUS_MAX, cur + JianXinGuPassiveTuning.FOCUS_GAIN_PER_CONTROL);
    state.setInt(K_FOCUS_METER, next);
    state.setLong(K_LAST_FOCUS_GAIN_AT, now);

    if (next >= JianXinGuPassiveTuning.FOCUS_MAX) {
      triggerResolve(player, state, now);
    }
  }

  /** 每秒调用：若超过1秒没有新增控制效果，则按配置衰减。*/
  public static void decayFocusIfIdle(ServerPlayer player, OrganState state) {
    if (player == null || state == null) return;
    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    long last = state.getLong(K_LAST_FOCUS_GAIN_AT, 0L);
    if (now - last < 20L) {
      return; // 1秒内有新增控制，不衰减
    }
    int cur = state.getInt(K_FOCUS_METER, 0);
    if (cur <= 0) return;
    int next = Math.max(0, cur - JianXinGuPassiveTuning.FOCUS_DECAY_PER_SEC);
    state.setInt(K_FOCUS_METER, next);
  }

  /** 满值触发“剑心定”。*/
  public static void triggerResolve(ServerPlayer player, OrganState state, long now) {
    // 清负面
    clearNegativeEffects(player);
    // 清空剑势
    state.setInt(K_SWORD_MOMENTUM, 0);
    // 授予“无视打断”
    DomainTags.grantUnbreakableFocus(player, JianXinGuPassiveTuning.UNBREAKABLE_FOCUS_T);
    // 锁定30s
    state.setLong(K_FOCUS_LOCK_UNTIL, now + JianXinGuPassiveTuning.FOCUS_LOCK_T);
    // 归零定心值
    state.setInt(K_FOCUS_METER, 0);

    // 冥想协同：若处于冥想，领域增益/减益翻倍2秒
    boolean meditating = state.getBoolean(K_MEDITATING, false);
    if (meditating) {
      var domain = DomainHelper.getJianXinDomain(player);
      if (domain != null) {
        domain.triggerEnhancement();
      }
    }
  }
}
