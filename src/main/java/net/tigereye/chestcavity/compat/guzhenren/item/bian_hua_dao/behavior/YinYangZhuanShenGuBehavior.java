package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Anchor;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment.DualStrikeWindow;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 阴阳转身蛊（变化道·四转） — 第一阶段实现：双态切换与太极错位。 */
public final class YinYangZhuanShenGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener {

  public static final YinYangZhuanShenGuBehavior INSTANCE = new YinYangZhuanShenGuBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu");

  private static final ResourceLocation SKILL_BODY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/body");
  private static final ResourceLocation SKILL_SWAP_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/tai_ji_swap");
  private static final ResourceLocation SKILL_DUAL_STRIKE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/dual_strike");
  private static final ResourceLocation SKILL_TRANSFER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/transfer");
  private static final ResourceLocation SKILL_RECALL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/recall");

  private static final long BODY_COOLDOWN_TICKS = 120L * 20L;
  private static final long SWAP_COOLDOWN_TICKS = 25L * 20L;
  private static final long SWAP_WINDOW_EXTENSION_TICKS = 40L; // 2 秒
  private static final long FALL_GUARD_TICKS = 60L; // 3 秒
  private static final float FALL_REDUCTION = 0.7f;

  private static final ResourceLocation MAX_HEALTH_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/max_health_yang");
  private static final ResourceLocation MAX_HEALTH_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/max_health_yin");
  private static final ResourceLocation ATTACK_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/attack_yang");
  private static final ResourceLocation ATTACK_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/attack_yin");
  private static final ResourceLocation ARMOR_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/armor_yang");
  private static final ResourceLocation ARMOR_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/armor_yin");
  private static final ResourceLocation MOVE_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/move_yang");
  private static final ResourceLocation MOVE_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/move_yin");
  private static final ResourceLocation KNOCKBACK_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/knockback_yang");

  private static final ResourceCost COST_BODY =
      new ResourceCost(200.0, 10.0, 5.0, 5.0, 5, 2.0f);
  private static final ResourceCost COST_SWAP = new ResourceCost(80.0, 8.0, 0.0, 3.0, 0, 0.0f);
  private static final ResourceCost COST_DUAL_STRIKE =
      new ResourceCost(120.0, 6.0, 0.0, 4.0, 0, 0.0f);
  private static final ResourceCost COST_TRANSFER =
      new ResourceCost(60.0, 0.0, 0.0, 6.0, 0, 0.0f);
  private static final ResourceCost COST_RECALL =
      new ResourceCost(70.0, 5.0, 0.0, 0.0, 0, 0.0f);

  private static final long DUAL_STRIKE_COOLDOWN_TICKS = 35L * 20L;
  private static final long DUAL_STRIKE_WINDOW_TICKS = 5L * 20L;
  private static final long TRANSFER_COOLDOWN_TICKS = 40L * 20L;
  private static final long RECALL_COOLDOWN_TICKS = 45L * 20L;
  private static final double TRANSFER_RATIO = 0.3D;

  static {
    OrganActivationListeners.register(
        SKILL_BODY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateBody(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_SWAP_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateSwap(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_DUAL_STRIKE_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateDualStrike(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_TRANSFER_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateTransfer(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_RECALL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateRecall(player, cc);
          }
        });
  }

  private YinYangZhuanShenGuBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player)) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    if (cc == null || cc.inventory == null) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    ensureAnchorPresent(player, attachment);
    applyModeAttributes(player, attachment);
    clampHealth(player);
    YinYangDualityOps.openHandle(player)
        .ifPresent(
            handle -> {
              runPassives(player, attachment, handle);
              attachment.pool(attachment.currentMode()).capture(player, handle);
            });
    long now = player.level().getGameTime();
    if (!attachment.dualStrike().isActive(now)) {
      attachment.dualStrike().clear();
    }
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(entity instanceof ServerPlayer player)) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || source == null || damage <= 0.0f) {
      return damage;
    }
    Optional<YinYangDualityAttachment> attachmentOpt = YinYangDualityOps.get(player);
    if (attachmentOpt.isEmpty()) {
      return damage;
    }
    YinYangDualityAttachment attachment = attachmentOpt.get();
    long now = player.level().getGameTime();
    if (now > attachment.fallGuardEndTick()) {
      return damage;
    }
    boolean fallRelated =
        source.is(DamageTypeTags.IS_FALL)
            || source == player.damageSources().flyIntoWall()
            || source == player.damageSources().cramming();
    if (!fallRelated) {
      return damage;
    }
    return damage * FALL_REDUCTION;
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof ServerPlayer player) || target == null || !target.isAlive()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    Optional<YinYangDualityAttachment> attachmentOpt = YinYangDualityOps.get(player);
    if (attachmentOpt.isEmpty()) {
      return damage;
    }
    handleDualStrikeHit(player, target, attachmentOpt.get());
    return damage;
  }

  private void activateBody(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !hasOrganEquipped(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (attachment.sealEndTick() > now) {
      sendFailure(player, "封印尚未解除，无法切换阴阳身。");
      return;
    }
    long readyAt = attachment.getCooldown(SKILL_BODY_ID);
    if (readyAt > now) {
      return;
    }
    if (!payCost(player, COST_BODY, "资源不足，无法切换阴阳身。")) {
      return;
    }
    YinYangDualityOps.captureAnchor(player)
        .ifPresent(anchor -> attachment.setAnchor(attachment.currentMode(), anchor));
    Mode next = attachment.currentMode().opposite();
    if (!YinYangDualityOps.swapPools(player, attachment, next)) {
      sendFailure(player, "无法同步阴阳资源，切态终止。");
      return;
    }
    attachment.setCurrentMode(next);
    applyModeAttributes(player, attachment);
    clampHealth(player);
    YinYangDualityOps.captureAnchor(player).ifPresent(anchor -> attachment.setAnchor(next, anchor));
    attachment.setFallGuardEndTick(now + FALL_GUARD_TICKS);
    long nextReady = now + BODY_COOLDOWN_TICKS;
    attachment.setCooldown(SKILL_BODY_ID, nextReady);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_BODY_ID, nextReady, now);
    sendAction(player, next == Mode.YANG ? "阳身护体" : "阴身出鞘");

    // FX: 阴阳身切换粒子效果
    playBodySwitchFx(player, next);
  }

  private void activateSwap(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !hasOrganEquipped(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    long readyAt = attachment.getCooldown(SKILL_SWAP_ID);
    if (readyAt > now) {
      return;
    }
    if (!payCost(player, COST_SWAP, "资源不足，无法施展太极错位。")) {
      return;
    }
    Mode currentMode = attachment.currentMode();
    Mode otherMode = currentMode.opposite();
    Anchor destination = attachment.anchor(otherMode);
    if (destination == null || !destination.isValid()) {
      sendFailure(player, "尚未记录另一态锚点，先使用“阴阳身”建立基点。");
      return;
    }
    Optional<Anchor> originOpt = YinYangDualityOps.captureAnchor(player);
    if (originOpt.isEmpty()) {
      return;
    }
    // 记录起点位置用于粒子效果
    Vec3 originPos = player.position();

    if (!YinYangDualityOps.teleportToAnchor(player, destination)) {
      sendFailure(player, "目标锚点不可达，太极错位失败。");
      return;
    }
    // 互换锚点
    attachment.setAnchor(otherMode, originOpt.get());
    YinYangDualityOps.captureAnchor(player).ifPresent(anchor -> attachment.setAnchor(currentMode, anchor));

    boolean withinWindow = now <= attachment.swapWindowEndTick();
    if (!withinWindow) {
      attachment.setSwapWindowEndTick(now + SWAP_WINDOW_EXTENSION_TICKS);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
    }
    attachment.setFallGuardEndTick(now + FALL_GUARD_TICKS);
    long cooldown =
        withinWindow ? Math.max(10L, SWAP_COOLDOWN_TICKS / 2) : SWAP_COOLDOWN_TICKS;
    long readyTick = now + cooldown;
    attachment.setCooldown(SKILL_SWAP_ID, readyTick);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_SWAP_ID, readyTick, now);

    // FX: 太极错位粒子效果
    playSwapFx(player, originPos, player.position(), withinWindow);
  }

  private void activateDualStrike(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !hasOrganEquipped(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (attachment.getCooldown(SKILL_DUAL_STRIKE_ID) > now) {
      return;
    }
    if (!payCost(player, COST_DUAL_STRIKE, "资源不足，无法施展两界同击。")) {
      return;
    }
    DualStrikeWindow window = attachment.dualStrike();
    window.clear();
    window.start(null, now + DUAL_STRIKE_WINDOW_TICKS);
    window.setBaseAttacks(
        attachment.pool(Mode.YIN).attackSnapshot(), attachment.pool(Mode.YANG).attackSnapshot());
    long ready = now + DUAL_STRIKE_COOLDOWN_TICKS;
    attachment.setCooldown(SKILL_DUAL_STRIKE_ID, ready);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_DUAL_STRIKE_ID, ready, now);
    sendAction(player, "两界同击窗口开启 5 秒");

    // FX: 两界同击窗口开启
    playDualStrikeActivateFx(player);
  }

  private void activateTransfer(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !hasOrganEquipped(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (attachment.getCooldown(SKILL_TRANSFER_ID) > now) {
      return;
    }
    if (!payCost(player, COST_TRANSFER, "资源不足，无法施展阴阳互渡。")) {
      return;
    }
    Optional<ResourceHandle> handleOpt = YinYangDualityOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法读取真元附件，互渡失败。");
      return;
    }
    if (!performTransfer(player, attachment, handleOpt.get())) {
      sendFailure(player, "资源不足以完成 30% 互渡。");
      return;
    }
    long ready = now + TRANSFER_COOLDOWN_TICKS;
    attachment.setCooldown(SKILL_TRANSFER_ID, ready);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_TRANSFER_ID, ready, now);
    sendAction(player, "阴阳互渡完成");

    // FX: 阴阳互渡粒子效果
    playTransferFx(player);
  }

  private void activateRecall(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null || !hasOrganEquipped(cc)) {
      return;
    }
    YinYangDualityAttachment attachment = YinYangDualityOps.resolve(player);
    long now = player.level().getGameTime();
    if (attachment.getCooldown(SKILL_RECALL_ID) > now) {
      return;
    }
    if (!payCost(player, COST_RECALL, "资源不足，无法归位。")) {
      return;
    }
    Mode other = attachment.currentMode().opposite();
    Anchor anchor = attachment.anchor(other);
    if (anchor == null || !anchor.isValid()) {
      sendFailure(player, "另一态未设置锚点，无法归位。");
      return;
    }
    double currentHealthRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
    if (!YinYangDualityOps.teleportToAnchor(player, anchor)) {
      sendFailure(player, "锚点不可达，归位失败。");
      return;
    }
    clearNearbyAggro(player);
    if (currentHealthRatio < 0.2D) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
    }
    long ready = now + RECALL_COOLDOWN_TICKS;
    attachment.setCooldown(SKILL_RECALL_ID, ready);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_RECALL_ID, ready, now);

    // FX: 归位粒子效果
    playRecallFx(player);
  }

  private boolean performTransfer(
      ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle) {
    Mode current = attachment.currentMode();
    Mode other = current.opposite();
    attachment.pool(other).ensureInitializedFrom(attachment.pool(current));
    double moved = 0.0D;
    double currentZhenyuan = handle.getZhenyuan().orElse(0.0D);
    double sendZhenyuan = currentZhenyuan * TRANSFER_RATIO;
    if (sendZhenyuan > 0.0D) {
      double accepted = attachment.pool(other).receiveZhenyuan(sendZhenyuan);
      if (accepted > 0.0D) {
        handle.adjustZhenyuan(-accepted, true);
        moved += accepted;
      }
    }

    double currentJingli = handle.getJingli().orElse(0.0D);
    double sendJingli = currentJingli * TRANSFER_RATIO;
    if (sendJingli > 0.0D) {
      double accepted = attachment.pool(other).receiveJingli(sendJingli);
      if (accepted > 0.0D) {
        handle.adjustJingli(-accepted, true);
        moved += accepted;
      }
    }

    double currentHunpo = handle.getHunpo().orElse(0.0D);
    double sendHunpo = currentHunpo * TRANSFER_RATIO;
    if (sendHunpo > 0.0D) {
      double accepted = attachment.pool(other).receiveSoul(sendHunpo);
      if (accepted > 0.0D) {
        handle.adjustHunpo(-accepted, true);
        moved += accepted;
      }
    }

    double currentNian = handle.getNiantou().orElse(0.0D);
    double sendNian = currentNian * TRANSFER_RATIO;
    if (sendNian > 0.0D) {
      double accepted = attachment.pool(other).receiveNiantou(sendNian);
      if (accepted > 0.0D) {
        handle.adjustNiantou(-accepted, true);
        moved += accepted;
      }
    }
    return moved > 0.0D;
  }

  private void clearNearbyAggro(ServerPlayer player) {
    AABB box = player.getBoundingBox().inflate(8.0D);
    player
        .level()
        .getEntitiesOfClass(
            Mob.class, box, mob -> mob.getTarget() == player && mob.isAlive() && !mob.isRemoved())
        .forEach(mob -> mob.setTarget(null));
  }

  private void handleDualStrikeHit(
      ServerPlayer player, LivingEntity target, YinYangDualityAttachment attachment) {
    long now = player.level().getGameTime();
    DualStrikeWindow window = attachment.dualStrike();
    if (!window.isActive(now)) {
      return;
    }
    if (!window.matchOrSetTarget(target.getUUID())) {
      return;
    }
    Mode mode = attachment.currentMode();
    if (mode == Mode.YIN) {
      if (window.yinHit()) {
        return;
      }
      window.markYinHit();
    } else {
      if (window.yangHit()) {
        return;
      }
      window.markYangHit();
    }
    // FX: 命中粒子效果
    playDualStrikeHitFx(player, target, window.yinHit() && window.yangHit());

    if (window.yinHit() && window.yangHit()) {
      double base =
          Math.max(0.0D, Math.min(window.baseAttackYin(), window.baseAttackYang()) * 0.8D);
      if (base > 0.0D) {
        target.hurt(player.damageSources().magic(), (float) base);
      }
      if (mode == Mode.YANG) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
      }
      window.clear();
    }
  }

  private void ensureAnchorPresent(ServerPlayer player, YinYangDualityAttachment attachment) {
    if (attachment.anchor(attachment.currentMode()).isValid()) {
      return;
    }
    YinYangDualityOps.captureAnchor(player)
        .ifPresent(anchor -> attachment.setAnchor(attachment.currentMode(), anchor));
  }

  private void applyModeAttributes(ServerPlayer player, YinYangDualityAttachment attachment) {
    Mode mode = attachment.currentMode();
    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      AttributeOps.removeById(maxHealth, MAX_HEALTH_YANG_MODIFIER);
      AttributeOps.removeById(maxHealth, MAX_HEALTH_YIN_MODIFIER);
      double amount = mode == Mode.YANG ? 1.0D : -0.5D;
      ResourceLocation id =
          mode == Mode.YANG ? MAX_HEALTH_YANG_MODIFIER : MAX_HEALTH_YIN_MODIFIER;
      AttributeModifier modifier =
          new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      AttributeOps.replaceTransient(maxHealth, id, modifier);
    }

    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      AttributeOps.removeById(attack, ATTACK_YANG_MODIFIER);
      AttributeOps.removeById(attack, ATTACK_YIN_MODIFIER);
      double amount = mode == Mode.YANG ? -0.25D : 1.0D;
      ResourceLocation id = mode == Mode.YANG ? ATTACK_YANG_MODIFIER : ATTACK_YIN_MODIFIER;
      AttributeModifier modifier =
          new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      AttributeOps.replaceTransient(attack, id, modifier);
    }

    AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
    if (armor != null) {
      AttributeOps.removeById(armor, ARMOR_YANG_MODIFIER);
      AttributeOps.removeById(armor, ARMOR_YIN_MODIFIER);
      double amount = mode == Mode.YANG ? 1.0D : -0.5D;
      ResourceLocation id = mode == Mode.YANG ? ARMOR_YANG_MODIFIER : ARMOR_YIN_MODIFIER;
      AttributeModifier modifier =
          new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      AttributeOps.replaceTransient(armor, id, modifier);
    }

    AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movement != null) {
      AttributeOps.removeById(movement, MOVE_YANG_MODIFIER);
      AttributeOps.removeById(movement, MOVE_YIN_MODIFIER);
      double amount = mode == Mode.YANG ? -0.25D : 0.333333D;
      ResourceLocation id = mode == Mode.YANG ? MOVE_YANG_MODIFIER : MOVE_YIN_MODIFIER;
      AttributeModifier modifier =
          new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      AttributeOps.replaceTransient(movement, id, modifier);
    }

    AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (knockback != null) {
      AttributeOps.removeById(knockback, KNOCKBACK_YANG_MODIFIER);
      if (mode == Mode.YANG) {
        AttributeModifier modifier =
            new AttributeModifier(
                KNOCKBACK_YANG_MODIFIER,
                0.5D,
                AttributeModifier.Operation.ADD_VALUE);
        AttributeOps.replaceTransient(knockback, KNOCKBACK_YANG_MODIFIER, modifier);
      }
    }
    attachment.pool(mode).setAttackSnapshot(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
  }

  private boolean hasOrganEquipped(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    Item targetItem = BuiltInRegistries.ITEM.getOptional(ORGAN_ID).orElse(null);
    if (targetItem == null) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == targetItem) {
        return true;
      }
    }
    return false;
  }

  private void clampHealth(ServerPlayer player) {
    AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
    if (attr == null) {
      return;
    }
    double max = attr.getValue();
    if (player.getHealth() > max) {
      player.setHealth((float) max);
    }
  }

  private void runPassives(
      ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle) {
    Mode mode = attachment.currentMode();
    FoodData foodData = player.getFoodData();
    if (mode == Mode.YANG) {
      handle.adjustJingli(10.0D, true);
      player.heal(20.0F);
      foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - 5));
      handle.adjustHunpo(-1.0D, true);
      handle.adjustNiantou(-1.0D, true);
      // FX: 阳身充盈被动效果（每2秒触发一次避免刷屏）
      if (player.level().getGameTime() % 40L == 0) {
        playPassiveYangFx(player);
      }
    } else {
      handle.adjustHunpo(20.0D, true);
      handle.adjustNiantou(2.0D, true);
      handle.adjustZhenyuan(10.0D, true);
      handle.adjustJingli(-1.0D, true);
      foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - 5));
      // FX: 阴身幽养被动效果（每2秒触发一次避免刷屏）
      if (player.level().getGameTime() % 40L == 0) {
        playPassiveYinFx(player);
      }
    }
  }

  private boolean payCost(ServerPlayer player, ResourceCost cost, String failureHint) {
    if (cost.isZero()) {
      return true;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, failureHint);
      return false;
    }
    ResourceHandle handle = handleOpt.get();
    double spentZhenyuan = 0.0D;
    boolean spentJingli = false;
    boolean spentHunpo = false;
    boolean spentNiantou = false;
    FoodData foodData = player.getFoodData();
    int prevFood = foodData.getFoodLevel();
    float prevSaturation = foodData.getSaturationLevel();
    float prevHealth = player.getHealth();
    boolean success = false;
    try {
      if (cost.zhenyuan() > 0.0D) {
        OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(handle, cost.zhenyuan());
        if (consumed.isEmpty()) {
          return fail(player, failureHint);
        }
        spentZhenyuan = consumed.getAsDouble();
      }
      if (cost.jingli() > 0.0D) {
        if (handle.adjustJingli(-cost.jingli(), true).isEmpty()) {
          return fail(player, failureHint);
        }
        spentJingli = true;
      }
      if (cost.hunpo() > 0.0D) {
        if (handle.adjustHunpo(-cost.hunpo(), true).isEmpty()) {
          return fail(player, failureHint);
        }
        spentHunpo = true;
      }
      if (cost.niantou() > 0.0D) {
        if (handle.adjustNiantou(-cost.niantou(), true).isEmpty()) {
          return fail(player, failureHint);
        }
        spentNiantou = true;
      }
      if (cost.hunger() > 0) {
        if (foodData.getFoodLevel() < cost.hunger()) {
          return fail(player, failureHint);
        }
        foodData.setFoodLevel(foodData.getFoodLevel() - cost.hunger());
      }
      if (cost.health() > 0.0f) {
        if (player.getHealth() <= cost.health() + 1.0f) {
          return fail(player, failureHint);
        }
        if (!ResourceOps.drainHealth(player, cost.health())) {
          return fail(player, failureHint);
        }
      }
      success = true;
      return true;
    } finally {
      if (!success) {
        if (spentZhenyuan > 0.0D) {
          handle.adjustZhenyuan(spentZhenyuan, true);
        }
        if (spentJingli) {
          handle.adjustJingli(cost.jingli(), true);
        }
        if (spentHunpo) {
          handle.adjustHunpo(cost.hunpo(), true);
        }
        if (spentNiantou) {
          handle.adjustNiantou(cost.niantou(), true);
        }
        foodData.setFoodLevel(prevFood);
        foodData.setSaturation(prevSaturation);
        player.setHealth(prevHealth);
      }
    }
  }

  private boolean fail(ServerPlayer player, String message) {
    sendFailure(player, message);
    return false;
  }

  private void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  private void sendAction(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  // ========== FX 粒子效果实现 ==========

  /** 阴阳身切换粒子效果 */
  private void playBodySwitchFx(ServerPlayer player, Mode newMode) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 入口：ENCHANT 围绕腰部顺时针扇形升腾
    for (int i = 0; i < 30; i++) {
      double angle = (i / 30.0) * Math.PI * 2;
      double offsetX = Math.cos(angle) * 0.6;
      double offsetZ = Math.sin(angle) * 0.6;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          x + offsetX,
          y + 1.0,
          z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.02);
    }

    if (newMode == Mode.YIN) {
      // 阴态：SOUL + PORTAL 从足底外扩
      for (int i = 0; i < 30; i++) {
        double angle = Math.random() * Math.PI * 2;
        double radius = Math.random() * 1.6;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        level.sendParticles(ParticleTypes.SOUL, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.01);
      }
      for (int i = 0; i < 24; i++) {
        double angle = Math.random() * Math.PI * 2;
        double radius = Math.random() * 1.6;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        level.sendParticles(ParticleTypes.PORTAL, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.02);
      }
    } else {
      // 阳态：SWEEP_ATTACK 环形 + CLOUD 短雾
      for (int i = 0; i < 12; i++) {
        double angle = (i / 12.0) * Math.PI * 2;
        double offsetX = Math.cos(angle) * 1.2;
        double offsetZ = Math.sin(angle) * 1.2;
        level.sendParticles(
            ParticleTypes.SWEEP_ATTACK,
            x + offsetX,
            y + 0.5,
            z + offsetZ,
            1,
            0.0,
            0.0,
            0.0,
            0.0);
      }
      for (int i = 0; i < 20; i++) {
        double offsetX = (Math.random() - 0.5) * 1.5;
        double offsetY = Math.random() * 1.0;
        double offsetZ = (Math.random() - 0.5) * 1.5;
        level.sendParticles(ParticleTypes.CLOUD, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.01);
      }
    }

    // 收束：END_ROD 上升流
    for (int i = 0; i < 12; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + 0.5, z + offsetZ, 1, 0.0, 0.15, 0.0, 0.05);
    }

    // 音效
    level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.0f);
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
  }

  /** 太极错位粒子效果 */
  private void playSwapFx(ServerPlayer player, Vec3 origin, Vec3 destination, boolean withinWindow) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    // 起点门：PORTAL + REVERSE_PORTAL
    for (int i = 0; i < 20; i++) {
      double offsetX = (Math.random() - 0.5) * 1.2;
      double offsetY = Math.random() * 2.0;
      double offsetZ = (Math.random() - 0.5) * 1.2;
      level.sendParticles(
          ParticleTypes.PORTAL,
          origin.x + offsetX,
          origin.y + offsetY,
          origin.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }
    for (int i = 0; i < 16; i++) {
      double offsetX = (Math.random() - 0.5) * 1.2;
      double offsetY = Math.random() * 2.0;
      double offsetZ = (Math.random() - 0.5) * 1.2;
      level.sendParticles(
          ParticleTypes.REVERSE_PORTAL,
          origin.x + offsetX,
          origin.y + offsetY,
          origin.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 终点门：PORTAL + REVERSE_PORTAL
    for (int i = 0; i < 20; i++) {
      double offsetX = (Math.random() - 0.5) * 1.2;
      double offsetY = Math.random() * 2.0;
      double offsetZ = (Math.random() - 0.5) * 1.2;
      level.sendParticles(
          ParticleTypes.PORTAL,
          destination.x + offsetX,
          destination.y + offsetY,
          destination.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }
    for (int i = 0; i < 16; i++) {
      double offsetX = (Math.random() - 0.5) * 1.2;
      double offsetY = Math.random() * 2.0;
      double offsetZ = (Math.random() - 0.5) * 1.2;
      level.sendParticles(
          ParticleTypes.REVERSE_PORTAL,
          destination.x + offsetX,
          destination.y + offsetY,
          destination.z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.05);
    }

    // 切割轨迹：两点连线 END_ROD
    Vec3 direction = destination.subtract(origin);
    double distance = direction.length();
    Vec3 step = direction.normalize().scale(0.6);
    int steps = Math.min(16, (int) (distance / 0.6));
    for (int i = 0; i < steps; i++) {
      Vec3 point = origin.add(step.scale(i));
      level.sendParticles(
          ParticleTypes.END_ROD,
          point.x,
          point.y + 1.0,
          point.z,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }

    // 无敌提示：头顶闪烁火花
    if (!withinWindow) {
      for (int i = 0; i < 6; i++) {
        double offsetX = (Math.random() - 0.5) * 0.3;
        double offsetZ = (Math.random() - 0.5) * 0.3;
        level.sendParticles(
            ParticleTypes.SMALL_FLAME,
            destination.x + offsetX,
            destination.y + 2.2,
            destination.z + offsetZ,
            1,
            0.0,
            0.0,
            0.0,
            0.02);
      }
    }

    // 音效
    level.playSound(
        null,
        destination.x,
        destination.y,
        destination.z,
        SoundEvents.ENDERMAN_TELEPORT,
        SoundSource.PLAYERS,
        1.0f,
        1.0f);
    if (withinWindow) {
      level.playSound(
          null,
          destination.x,
          destination.y,
          destination.z,
          SoundEvents.EXPERIENCE_ORB_PICKUP,
          SoundSource.PLAYERS,
          0.6f,
          1.5f);
    }
  }

  /** 两界同击窗口开启粒子效果 */
  private void playDualStrikeActivateFx(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 胸前漂浮 ENCHANT 微粒
    for (int i = 0; i < 8; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = 1.2 + (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(ParticleTypes.ENCHANT, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.02, 0.0, 0.01);
    }

    // 音效
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.7f);
  }

  /** 两界同击命中粒子效果 */
  private void playDualStrikeHitFx(ServerPlayer player, LivingEntity target, boolean bothHit) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.7, 0);
    Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);

    // 弹道拖尾：CRIT
    Vec3 direction = targetPos.subtract(playerPos);
    double distance = direction.length();
    Vec3 step = direction.normalize().scale(0.3);
    int steps = Math.min(12, (int) (distance / 0.3));
    for (int i = 0; i < steps; i++) {
      Vec3 point = playerPos.add(step.scale(i));
      level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // 命中点：CRIT + ELECTRIC_SPARK
    for (int i = 0; i < 10; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.CRIT,
          targetPos.x + offsetX,
          targetPos.y + offsetY,
          targetPos.z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.05);
    }
    for (int i = 0; i < 8; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          targetPos.x + offsetX,
          targetPos.y + offsetY,
          targetPos.z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.05);
    }

    // 投影触发：ENCHANT 向外炸散
    if (bothHit) {
      for (int i = 0; i < 18; i++) {
        double angle = Math.random() * Math.PI * 2;
        double radius = Math.random() * 1.0;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = (Math.random() - 0.5) * 0.8;
        level.sendParticles(
            ParticleTypes.ENCHANT,
            targetPos.x + offsetX,
            targetPos.y + offsetY,
            targetPos.z + offsetZ,
            1,
            offsetX * 0.2,
            offsetY * 0.2,
            offsetZ * 0.2,
            0.1);
      }
      // 音效
      level.playSound(
          null,
          targetPos.x,
          targetPos.y,
          targetPos.z,
          SoundEvents.TRIDENT_THUNDER,
          SoundSource.PLAYERS,
          0.6f,
          1.25f);
    }
  }

  /** 阴阳互渡粒子效果 */
  private void playTransferFx(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 引导：胸口 END_ROD 涌出
    Vec3 lookDir = player.getLookAngle();
    for (int i = 0; i < 15; i++) {
      double dist = i * 0.5;
      double offsetX = lookDir.x * dist + (Math.random() - 0.5) * 0.3;
      double offsetY = 1.2 + lookDir.y * dist + (Math.random() - 0.5) * 0.3;
      double offsetZ = lookDir.z * dist + (Math.random() - 0.5) * 0.3;
      if (dist > 8.0) break;
      level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // 抵达：ENCHANT 漩涡 + SOUL
    Vec3 endpoint = pos.add(lookDir.scale(Math.min(8.0, 4.0)));
    for (int i = 0; i < 12; i++) {
      double angle = (i / 12.0) * Math.PI * 2;
      double radius = 0.6;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          endpoint.x + offsetX,
          endpoint.y + 1.0,
          endpoint.z + offsetZ,
          1,
          -offsetX * 0.1,
          0.0,
          -offsetZ * 0.1,
          0.05);
    }
    for (int i = 0; i < 8; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.SOUL,
          endpoint.x + offsetX,
          endpoint.y + 1.0 + offsetY,
          endpoint.z + offsetZ,
          1,
          0.0,
          0.05,
          0.0,
          0.02);
    }

    // 音效
    level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
  }

  /** 归位粒子效果 */
  private void playRecallFx(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 回环：脚下 CLOUD 圆环 + SWEEP_ATTACK
    for (int i = 0; i < 18; i++) {
      double angle = (i / 18.0) * Math.PI * 2;
      double offsetX = Math.cos(angle) * 1.2;
      double offsetZ = Math.sin(angle) * 1.2;
      level.sendParticles(ParticleTypes.CLOUD, x + offsetX, y + 0.1, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.0);
    }
    for (int i = 0; i < 8; i++) {
      double angle = (i / 8.0) * Math.PI * 2;
      double offsetX = Math.cos(angle) * 1.0;
      double offsetZ = Math.sin(angle) * 1.0;
      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK,
          x + offsetX,
          y + 0.3,
          z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.0);
    }

    // 锚点闪烁：GLOW + END_ROD
    for (int i = 0; i < 10; i++) {
      double offsetX = (Math.random() - 0.5) * 1.0;
      double offsetY = Math.random() * 1.5;
      double offsetZ = (Math.random() - 0.5) * 1.0;
      level.sendParticles(ParticleTypes.GLOW, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.05, 0.0, 0.02);
    }
    for (int i = 0; i < 10; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(ParticleTypes.END_ROD, x + offsetX, y + 0.2, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.05);
    }

    // 音效
    level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.0f);
  }

  /** 阳身充盈被动粒子效果 */
  private void playPassiveYangFx(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 胸前 HEART + CLOUD
    level.sendParticles(ParticleTypes.HEART, x, y + 1.2, z, 1, 0.2, 0.1, 0.2, 0.0);
    level.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 1, 0.2, 0.1, 0.2, 0.01);
  }

  /** 阴身幽养被动粒子效果 */
  private void playPassiveYinFx(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // SOUL + ENCHANT 低密度火花
    level.sendParticles(ParticleTypes.SOUL, x, y + 1.2, z, 1, 0.2, 0.1, 0.2, 0.01);
    level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 1, 0.2, 0.1, 0.2, 0.01);
  }

  // ========== FX 粒子效果实现结束 ==========

  private record ResourceCost(
      double zhenyuan, double jingli, double hunpo, double niantou, int hunger, float health) {
    private boolean isZero() {
      return zhenyuan <= 0.0D
          && jingli <= 0.0D
          && hunpo <= 0.0D
          && niantou <= 0.0D
          && hunger <= 0
          && health <= 0.0f;
    }
  }
}
