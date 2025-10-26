package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
    } else {
      handle.adjustHunpo(20.0D, true);
      handle.adjustNiantou(2.0D, true);
      handle.adjustZhenyuan(10.0D, true);
      handle.adjustJingli(-1.0D, true);
      foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - 5));
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
