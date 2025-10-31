package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.runtime;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageCalculator;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageComputeContext;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageResult;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator.YuQunComboLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.fx.YuQunFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.messages.YuQunMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning.YuQunTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.ComboSkillEntry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 鱼群组合杀招运行时入口。 */
public final class YuQunRuntime {
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_qun_combo");

  private static final String COOLDOWN_KEY = "YuQunComboReadyAt";
  private static final double ZHENYUAN_COST = 120.0;
  private static final double JINGLI_COST = 12.0;
  private static final int HUNGER_COST = 2;

  private YuQunRuntime() {}

  public static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    ComboSkillEntry entry = ComboSkillRegistry.get(ABILITY_ID).orElse(null);
    if (entry == null) {
      return;
    }

    ItemStack organ = YuLinGuOps.findOrgan(player);
    if (organ.isEmpty()) {
      YuQunMessages.missingOrgan(player);
      return;
    }

    OrganState state = OrganState.of(organ, YuLinGuOps.stateRoot());
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(COOLDOWN_KEY).withDefault(0L);
    long now = serverLevel.getGameTime();
    if (!ready.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      YuQunMessages.insufficientZhenyuan(player);
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      YuQunMessages.insufficientJingli(player);
      return;
    }
    YuLinGuOps.drainHunger(player, HUNGER_COST);

    double waterDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_shuidao", 0.0D);
    double changeDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_bianhuadao", 0.0D);
    double fireDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_yandao", 0.0D);
    double waterFlowExp =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:liupai_shuidao", 0.0D);
    double changeFlowExp =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:liupai_bianhuadao", 0.0D);
    double totalExp = waterFlowExp + changeFlowExp;
    int synergyCount = ComboSkillRegistry.countOptionalSynergy(player, entry);

    YuQunComboLogic.Parameters params =
        YuQunComboLogic.computeParameters(waterDaoHen, changeDaoHen, fireDaoHen, synergyCount);

    Vec3 origin = player.getEyePosition();
    Vec3 dir = player.getLookAngle().normalize();
    double range = params.range();
    double width = params.width();
    AABB box = player.getBoundingBox().inflate(range, 2.0, range);
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            t -> t != player && t.isAlive() && !t.isAlliedTo(player) && !t.isInvulnerable());
    for (LivingEntity target : targets) {
      if (!YuQunComboLogic.isWithinCone(origin, dir, target.getEyePosition(), range, width)) {
        continue;
      }
      applyDamage(player, serverLevel, params, target, dir);
      applyCrowdControl(target, params);
      if (params.spawnSplashParticles()) {
        YuQunFx.playHit(serverLevel, target);
      }
    }

    YuQunFx.playCast(player);
    YuLinGuOps.recordWetContact(player, organ);

    int cooldownTicks = YuQunTuning.computeCooldownTicks(totalExp);
    long readyAt = now + cooldownTicks;
    ready.setReadyAt(readyAt);
    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void applyDamage(
      ServerPlayer player,
      ServerLevel level,
      YuQunComboLogic.Parameters params,
      LivingEntity target,
      Vec3 direction) {
    DamageComputeContext context =
        DamageComputeContext.builder(player, params.damage())
            .defender(target)
            .skill(ABILITY_ID)
            .addKind(DamageKind.AOE)
            .addKind(DamageKind.ACTIVE_SKILL)
            .addKind(DamageKind.COMBO)
            .build();
    DamageResult damageResult = DamageCalculator.compute(context);
    float appliedDamage = (float) damageResult.scaled();
    if (appliedDamage > 0.0f) {
      target.hurt(level.damageSources().playerAttack(player), appliedDamage);
    }
    double push = params.pushStrength();
    target.push(direction.x * push, 0.35, direction.z * push);
    target.hurtMarked = true;
  }

  private static void applyCrowdControl(LivingEntity target, YuQunComboLogic.Parameters params) {
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, params.slowDurationTicks(), params.slowAmplifier()));
  }
}
