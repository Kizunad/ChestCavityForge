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
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.behavior.DualStrikeBehavior;
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

  private static final long BODY_COOLDOWN_TICKS = 120L * 20L;
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


  static {
    OrganActivationListeners.register(
        SKILL_BODY_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateBody(player, cc);
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
    DualStrikeBehavior.handleHit(player, target, attachmentOpt.get());
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
