package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageCalculator;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageComputeContext;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageResult;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.calculator.YuLinGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator.YuQunComboLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning.YuQunTuning;

/** 鱼群组合杀招：复用原始鱼群技能的成本与冷却，并基于协同计算参数。 */
public final class YuQunComboBehavior {
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_qun_combo");

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");
  private static final String STATE_ROOT = "YuLinGu";
  private static final String COOLDOWN_KEY = "YuQunComboReadyAt";

  private static final double ZHENYUAN_COST = 120.0;
  private static final double JINGLI_COST = 12.0;
  private static final int HUNGER_COST = 2;

  static {
    OrganActivationListeners.register(ABILITY_ID, YuQunComboBehavior::activate);
  }

  private YuQunComboBehavior() {}

  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }

    ComboSkillRegistry.ComboSkillEntry entry = ComboSkillRegistry.get(ABILITY_ID).orElse(null);
    if (entry == null) {
      return;
    }

    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("缺少鱼鳞蛊，无法施展鱼群·组合"), true);
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
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
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("真元不足，鱼群溃散。"), true);
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("精力不足，鱼群溃散。"), true);
      return;
    }
    drainHunger(player, HUNGER_COST);

    // 获取道痕和流派经验
    double waterDaoHen = SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_shuidao", 0.0D);
    double changeDaoHen = SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_bianhuadao", 0.0D);
    double fireDaoHen = SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:daohen_yandao", 0.0D);
    double waterFlowExp = SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:liupai_shuidao", 0.0D);
    double changeFlowExp = SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_qun:liupai_bianhuadao", 0.0D);
    double totalExp = waterFlowExp + changeFlowExp;
    int synergyCount = ComboSkillRegistry.countOptionalSynergy(player, entry);

    // 计算技能参数
    YuQunComboLogic.Parameters params = YuQunComboLogic.computeParameters(waterDaoHen, changeDaoHen, fireDaoHen, synergyCount);
    double range = params.range();
    double width = params.width();

    Vec3 origin = player.getEyePosition();
    Vec3 dir = player.getLookAngle().normalize();
    AABB box = player.getBoundingBox().inflate(range, 2.0, range);
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            t -> t != player && t.isAlive() && !t.isAlliedTo(player) && !t.isInvulnerable());
    for (LivingEntity t : targets) {
      if (!YuQunComboLogic.isWithinCone(origin, dir, t.getEyePosition(), range, width)) {
        continue;
      }
      DamageComputeContext context =
          DamageComputeContext.builder(player, params.damage())
              .defender(t)
              .skill(ABILITY_ID)
              .addKind(DamageKind.AOE)
              .addKind(DamageKind.ACTIVE_SKILL)
              .addKind(DamageKind.COMBO)
              .build();
      DamageResult damageResult = DamageCalculator.compute(context);
      float appliedDamage = (float) damageResult.scaled();
      if (appliedDamage > 0.0f) {
        t.hurt(serverLevel.damageSources().playerAttack(player), appliedDamage);
      }
      double push = params.pushStrength();
      t.push(dir.x * push, 0.35, dir.z * push);
      t.hurtMarked = true;
      t.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, params.slowDurationTicks(), params.slowAmplifier()));
      if (params.spawnSplashParticles()) {
        serverLevel.sendParticles(
            ParticleTypes.SPLASH, t.getX(), t.getEyeY(), t.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
      }
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.9f, 1.05f);

    YuLinGuCalculator.recordWetContact(player, organ);

    // 计算并设置冷却
    int cooldownTicks = YuQunTuning.computeCooldownTicks(totalExp);
    long readyAt = now + cooldownTicks;
    ready.setReadyAt(readyAt);
    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void drainHunger(ServerPlayer player, int amount) {
    if (amount <= 0) {
      return;
    }
    var food = player.getFoodData();
    food.setFoodLevel(Math.max(0, food.getFoodLevel() - amount));
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
