package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.skills.JianYinGuSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianYinGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.command.SwordCommandCenter;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

/**
 * 剑引蛊行为占位：当前仅提供状态与事件桥接，便于后续填充实际逻辑。
 */
public enum JianYinGuOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
  private static final ResourceLocation LEDGER_GUIDANCE_STRENGTH =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_yin_guidance_strength");

  static {
    OrganActivationListeners.register(
        JianYinGuTuning.ABILITY_ID, JianYinGuOrganBehavior::activateAbility);
    OrganActivationListeners.register(
        JianYinGuTuning.ABILITY_UI_ID, JianYinGuOrganBehavior::activateCommandUi);
  }

  /** 确保持久化账本已建立。 */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    LedgerOps.ensureChannel(cc, LEDGER_GUIDANCE_STRENGTH, NON_NEGATIVE);
  }

  /** ServerTick -> PassiveEvents 入口。 */
  public void handlePassiveBridge(ServerPlayer player, ChestCavityInstance cc, long nowTick) {
    if (player == null || cc == null) {
      return;
    }
    findOrgan(cc).ifPresent(stack -> JianYinGuSkill.tickPassive(player, cc, stack, nowTick));
    SwordCommandCenter.tick(player, nowTick);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ)) {
      return;
    }

    long now = entity.level().getGameTime();
    OrganState state = OrganState.of(organ, JianYinGuTuning.STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    cooldown.entry(JianYinGuTuning.KEY_READY_TICK).withDefault(0L);

    JianYinGuSkill.tickPassive(player, cc, organ, now);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof ServerPlayer player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ)) {
      return damage;
    }
    JianYinGuSkill.onMeleeHit(player, cc, organ, target, damage);
    return damage;
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }

    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }
    ItemStack organ = organOpt.get();
    OrganState state = OrganState.of(organ, JianYinGuTuning.STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(JianYinGuTuning.KEY_READY_TICK).withDefault(0L);

    long now = player.level().getGameTime();

    long readyAt = ready.getReadyTick();
    if (readyAt > now) {
      JianYinGuFx.scheduleCooldownToast(player, JianYinGuTuning.ABILITY_ID, readyAt, now);
      return;
    }

    if (SwordCommandCenter.isSelectionActive(player)) {
      SwordCommandCenter.cancelSelection(player);
      player.sendSystemMessage(
          Component.translatable("message.guzhenren.jianyingu.scan.cancel"));
      JianYinGuFx.playCancelFx(player);
      // 取消选择时也设置短冷却，防止无限快速激活
      long cancelCooldown = now + (JianYinGuTuning.ACTIVE_COOLDOWN_T / 6); // 2秒冷却
      ready.setReadyAt(cancelCooldown);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
      return;
    }

    if (!JianYinGuSkill.castGuidance(player, cc, organ, now)) {
      return;
    }

    int marked = SwordCommandCenter.startSelection(player, now);
    if (marked > 0) {
      player.sendSystemMessage(
          Component.translatable("message.guzhenren.jianyingu.scan.marked", marked));
      JianYinGuFx.playScanFx(player, marked);
      SwordCommandCenter.openTui(player);
    } else {
      player.sendSystemMessage(
          Component.translatable("message.guzhenren.jianyingu.scan.none"));
      JianYinGuFx.playScanEmptyFx(player);
    }

    long nextReady = now + JianYinGuTuning.ACTIVE_COOLDOWN_T;
    ready.setReadyAt(nextReady);
    JianYinGuFx.scheduleCooldownToast(player, JianYinGuTuning.ABILITY_ID, nextReady, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void activateCommandUi(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }
    if (!hasOrgan(cc)) {
      return;
    }
    SwordCommandCenter.openTui(player);
    JianYinGuFx.playActivationFx(player);
  }

  public static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack)) {
        return Optional.of(stack);
      }
    }
    return Optional.empty();
  }

  public static boolean hasOrgan(ChestCavityInstance cc) {
    return findOrgan(cc).isPresent();
  }

  @Override
  public float onIncomingDamage(
      DamageSource source, LivingEntity owner, ChestCavityInstance cc, ItemStack organ, float dmg) {
    if (!(owner instanceof ServerPlayer player) || owner.level().isClientSide()) {
      return dmg;
    }
    if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
      return dmg;
    }
    if (dmg <= 0.0f) {
      return dmg;
    }

    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(serverLevel, player);
    List<FlyingSwordEntity> guardSwords =
        swords.stream().filter(s -> s.getAIMode() == AIMode.GUARD).toList();
    if (guardSwords.isEmpty()) {
      return dmg;
    }

    double baseChance = JianYinGuTuning.GUARD_BLOCK_BASE_CHANCE;
    net.minecraft.util.RandomSource random = player.getRandom();
    boolean trigger = false;
    for (int i = 0; i < guardSwords.size(); i++) {
      if (random.nextDouble() < baseChance) {
        trigger = true;
        break;
      }
    }
    if (!trigger) {
      return dmg;
    }

    FlyingSwordEntity blocker = chooseBlocker(guardSwords, player, source);
    if (blocker == null) {
      blocker = guardSwords.get(random.nextInt(guardSwords.size()));
    }

    applyGuardBlockEffects(player, guardSwords, blocker, source.getEntity(), dmg);
    return 0.0f;
  }

  private static FlyingSwordEntity chooseBlocker(
      List<FlyingSwordEntity> swords, ServerPlayer player, DamageSource source) {
    Vec3 attackDir = null;
    if (source.getEntity() != null) {
      attackDir = source.getEntity().position().subtract(player.position());
    } else if (source.getSourcePosition() != null) {
      attackDir = source.getSourcePosition().subtract(player.position());
    }
    if (attackDir == null || attackDir.lengthSqr() < 1.0e-4) {
      return null;
    }
    Vec3 normalizedAttack = attackDir.normalize();
    FlyingSwordEntity best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (FlyingSwordEntity sword : swords) {
      Vec3 offset = sword.position().subtract(player.position());
      double lenSq = offset.lengthSqr();
      if (lenSq < 1.0e-4) {
        continue;
      }
      Vec3 normalized = offset.normalize();
      double alignment = normalized.dot(normalizedAttack);
      double distancePenalty = Math.sqrt(lenSq) * 0.05;
      double score = alignment - distancePenalty;
      if (score > bestScore) {
        bestScore = score;
        best = sword;
      }
    }
    return best;
  }

  private static void applyGuardBlockEffects(
      ServerPlayer player,
      List<FlyingSwordEntity> guardSwords,
      FlyingSwordEntity blocker,
      net.minecraft.world.entity.Entity attacker,
      float damage) {
    if (blocker != null && damage > 0.0f) {
      float durabilityLoss = (float) (damage * JianYinGuTuning.GUARD_BLOCK_DURABILITY_RATIO);
      if (durabilityLoss > 0.0f) {
        blocker.damageDurability(durabilityLoss);
      }
    }

    for (FlyingSwordEntity sword : guardSwords) {
      sword.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SPEED,
              JianYinGuTuning.GUARD_BLOCK_SWORD_SPEED_DURATION_T,
              JianYinGuTuning.GUARD_BLOCK_SWORD_SPEED_AMPLIFIER,
              false,
              false));
    }

    if (attacker instanceof LivingEntity living) {
      living.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN,
              JianYinGuTuning.GUARD_BLOCK_ENEMY_DEBUFF_DURATION_T,
              JianYinGuTuning.GUARD_BLOCK_ENEMY_SLOW_AMPLIFIER,
              false,
              true));
      living.addEffect(
          new MobEffectInstance(
              MobEffects.WEAKNESS,
              JianYinGuTuning.GUARD_BLOCK_ENEMY_DEBUFF_DURATION_T,
              JianYinGuTuning.GUARD_BLOCK_ENEMY_WEAKNESS_AMPLIFIER,
              false,
              true));
    }

    player.sendSystemMessage(
        Component.translatable("message.guzhenren.jianyingu.guard.block"));
    JianYinGuFx.playGuardBlockFx(player, blocker, attacker);
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return JianYinGuTuning.ORGAN_ID.equals(id);
  }

}
