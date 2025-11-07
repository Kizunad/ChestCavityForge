package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.SwordShadowRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.AfterimageScheduler;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;

/** Behaviour for 剑影蛊. Handles passive shadow strikes, afterimages, and the sword clone ability. */
public enum JianYingGuOrganBehavior implements OrganOnHitListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_gu");
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_fenshen");
  private static final ResourceLocation SKILL_PASSIVE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_dao/shadow_strike");
  private static final ResourceLocation SKILL_AFTERIMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_dao/afterimage");

  private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_ROOT = "JianYingGu";
  private static final String ACTIVE_READY_KEY = "ActiveReadyAt";
  private static final String ON_HIT_READY_KEY = "OnHitReadyAt";


  static {
    OrganActivationListeners.register(ABILITY_ID, JianYingGuOrganBehavior::activateAbility);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
      return damage;
    }
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
      return damage;
    }
    if (target == null || !target.isAlive()) {
      return damage;
    }
    if (!isMeleeAttack(source)) {
      return damage;
    }

    double efficiency =
        1.0 + LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE).get();

    // OnHit 触发冷却：5 秒最多触发一次（MultiCooldown 存储于器官状态）
    ItemStack stateStack = organ == null || organ.isEmpty() ? findOrgan(cc) : organ;
    MultiCooldown cooldown =
        MultiCooldown.builder(OrganState.of(stateStack, STATE_ROOT)).withSync(cc, stateStack).build();
    MultiCooldown.Entry onHitCd = cooldown.entry(ON_HIT_READY_KEY);
    long nowTick = attacker.level().getGameTime();

    boolean triggeredAny = false;
    if (onHitCd.isReady(nowTick)) {
      double passiveDamage = SwordShadowRuntime.attemptPassiveStrike(player, target, efficiency);
      if (passiveDamage > 0.0) {
        triggeredAny = true;
        SwordShadowRuntime.applyTrueDamage(
            player,
            target,
            (float) passiveDamage,
            SKILL_PASSIVE_ID,
            java.util.Set.of(DamageKind.MELEE, DamageKind.TRUE_DAMAGE));
      }

      // 残影仅在冷却就绪时允许尝试
      if (SwordShadowRuntime.trySpawnAfterimage(player, target, source)) {
        triggeredAny = true;
      }

      if (triggeredAny) {
        onHitCd.setReadyAt(nowTick + JianYingTuning.ON_HIT_COOLDOWN_TICKS);
      }
    }

    // 分身 AI 指令不受 OnHit 冷却限制，保持响应性
    SwordShadowRuntime.commandClones(player, target);

    return damage;
  }

  public void ensureAttached(ChestCavityInstance cc) {
    LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE);
  }

  public void tickLevel(ServerLevel level) {
    AfterimageScheduler.tickLevel(level);
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!hasOrgan(cc)) {
      return;
    }
    int organCount = countOrgans(cc);
    ItemStack organStack = findOrgan(cc);
    SwordShadowRuntime.activateClone(entity, cc, organStack, organCount);
  }

  private static boolean hasOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return true;
      }
    }
    return false;
  }

  private static int countOrgans(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return 0;
    }
    int total = 0;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        total += Math.max(1, stack.getCount());
      }
    }
    return total;
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  public static int getCloneCooldownTicks() {
    return JianYingTuning.CLONE_COOLDOWN_TICKS;
  }

  private static boolean isMeleeAttack(DamageSource source) {
    return !source.is(DamageTypeTags.IS_PROJECTILE);
  }

  public static void markExternalCrit(Player player) {
    SwordShadowRuntime.markExternalCrit(player);
  }

  public static void applyTrueDamage(Player player, LivingEntity target, float amount) {
    SwordShadowRuntime.applyTrueDamage(player, target, amount);
  }

  public static void applyTrueDamage(
      Player player, LivingEntity target, float baseAmount, ResourceLocation skillId, Set<DamageKind> kinds) {
    SwordShadowRuntime.applyTrueDamage(player, target, baseAmount, skillId, kinds);
  }
}
