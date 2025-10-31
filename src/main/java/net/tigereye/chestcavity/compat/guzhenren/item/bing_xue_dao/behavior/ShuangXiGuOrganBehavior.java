package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.CooldownOps;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.DamageOverTimeHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
// ReactionEngine runtime逻辑已迁移至 engine/reaction；此类未直接使用可移除旧导入。
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behaviour for 霜息蛊 (Shuang Xi Gu). */
public final class ShuangXiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganOnGroundListener,
        OrganRemovalListener {

  public static final ShuangXiGuOrganBehavior INSTANCE = new ShuangXiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuang_xi_gu");
  private static final ResourceLocation BING_JI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
  private static final ResourceLocation BING_XUE_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_xue_dao_increase_effect");
  private static final ResourceLocation ICE_COLD_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hhanleng");

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuang_xi_gu_frost_breath");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig DEFAULTS =
      new CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig();

  private ShuangXiGuOrganBehavior() {}

  private static CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig cfg() {
    CCConfig root = ChestCavity.config;
    if (root != null) {
      CCConfig.GuzhenrenBingXueDaoConfig group = root.GUZHENREN_BING_XUE_DAO;
      if (group != null && group.SHUANG_XI_GU != null) {
        return group.SHUANG_XI_GU;
      }
    }
    return DEFAULTS;
  }

  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
      return;
    }

    registerRemovalHook(cc, organ, this, staleRemovalContexts);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
      return;
    }
    // refreshIncreaseContribution(cc, organ);

    if (!hasBingJiGu(cc)) {
      return;
    }

    CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config = cfg();
    clearColdEffects(entity, config);
    reduceFreezing(entity, config);
    maintainSnowStride(entity);
  }

  @Override
  public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
      return;
    }
    if (!hasBingJiGu(cc)) {
      return;
    }
    maintainSnowStride(entity);
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, CCItems.GUZHENREN_SHUANG_XI_GU, ORGAN_ID)) {
      return;
    }
  }

  public void ensureAttached(ChestCavityInstance cc) {
  }

  private static void clearColdEffects(
      LivingEntity entity, CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config) {
    Optional<Holder.Reference<MobEffect>> holder =
        BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
    holder.ifPresent(entity::removeEffect);
    entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    entity.removeEffect(MobEffects.DIG_SLOWDOWN);
  }

  private static void reduceFreezing(
      LivingEntity entity, CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config) {
    int frozen = entity.getTicksFrozen();
    if (frozen > 0) {
      int reduction = Math.max(0, config.freezeReductionTicks);
      entity.setTicksFrozen(Math.max(0, frozen - reduction));
    }
  }

  private static void maintainSnowStride(LivingEntity entity) {
    Level level = entity.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY, entity.getZ());
    BlockState state = level.getBlockState(pos);
    if (!state.is(Blocks.POWDER_SNOW)) {
      return;
    }
    double targetY = pos.getY() + 1.0D;
    if (entity.getY() < targetY) {
      entity.setPos(entity.getX(), targetY, entity.getZ());
    }
    Vec3 motion = entity.getDeltaMovement();
    if (motion.y < 0.0D) {
      entity.setDeltaMovement(motion.x, 0.0D, motion.z);
    }
    entity.fallDistance = 0.0F;
    server.sendParticles(
        ParticleTypes.SNOWFLAKE,
        entity.getX(),
        entity.getY() + entity.getBbHeight() * 0.5,
        entity.getZ(),
        4,
        0.1,
        0.1,
        0.1,
        0.01);
  }

  private static boolean hasBingJiGu(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      if (stack.is(CCItems.GUZHENREN_BING_JI_GU)) {
        return true;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, BING_JI_GU_ID)) {
        return true;
      }
    }
    return false;
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
      if (stack.is(CCItems.GUZHENREN_SHUANG_XI_GU)) {
        return stack;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
      if (entity == null || cc == null || entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
          return;
      }
      ItemStack organ = findOrgan(cc);
      if (organ.isEmpty()) {
          return;
      }

      // Read from snapshot
      double daohen = SkillEffectBus.consumeMetadata(player, ABILITY_ID, ComputedBingXueDaohenEffect.DAO_HEN_KEY, 0.0);
      int liupaiExp = (int) SkillEffectBus.consumeMetadata(player, ABILITY_ID, "bing_xue:liupai_bingxuedao", 0.0);
      double multiplier = 1.0 + Math.max(0.0, daohen);

      CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config = cfg();
      long cooldown = CooldownOps.withBingXueExp(config.baseCooldownTicks, liupaiExp);

      if (player.getCooldowns().isOnCooldown(organ.getItem())) {
          return;
      }

      var consume = ResourceOps.consumeStrict(entity, config.baseZhenyuanCost, 0.0);
      if (!consume.succeeded()) {
          if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "[compat/guzhenren][shuang_xi] ability blocked: zhenyuan cost={} failure={}",
                  config.baseZhenyuanCost,
                  consume.failureReason());
          }
          return;
      }
      Level level = entity.level();
      if (!(level instanceof ServerLevel server)) {
          return;
      }
      Vec3 origin = entity.getEyePosition();
      Vec3 look = entity.getLookAngle().normalize();
      double abilityRange = Math.max(0.0D, config.abilityRange);
      double coneThreshold = Mth.clamp(config.coneDotThreshold, -1.0D, 1.0D);
      double frostbiteChance = Mth.clamp(config.frostbiteChance, 0.0D, 1.0D);
      double frostbiteBasePercent = Math.max(0.0D, config.frostbiteDamagePercent);
      int frostbiteDurationSeconds = Math.max(0, config.frostbiteDurationSeconds);
      AABB search = entity.getBoundingBox().expandTowards(look.scale(abilityRange)).inflate(1.5);
      List<LivingEntity> candidates =
          server.getEntitiesOfClass(
              LivingEntity.class,
              search,
              target -> target != entity && target.isAlive() && !target.isAlliedTo(entity));
      List<LivingEntity> affected = new ArrayList<>();
      for (LivingEntity target : candidates) {
          Vec3 toTarget = target.getEyePosition().subtract(origin);
          double distance = toTarget.length();
          if (distance <= 0.0001D || distance > abilityRange) {
              continue;
          }
          Vec3 direction = toTarget.normalize();
          double dot = direction.dot(look);
          if (dot < coneThreshold) {
              continue;
          }
          affected.add(target);
          applyColdEffect(target, config);
          if (entity.getRandom().nextDouble() < frostbiteChance) {
              double percent = frostbiteBasePercent * multiplier;
              if (LOGGER.isInfoEnabled()) {
                  LOGGER.info(
                      "[compat/guzhenren][shuang_xi] apply frostbite DoT: base={} daohen={} final={} target={}",
                      frostbiteBasePercent,
                      daohen,
                      percent,
                      target.getName().getString());
              }
              DamageOverTimeHelper.applyBaseAttackPercentDoT(
                  entity,
                  target,
                  percent,
                  frostbiteDurationSeconds,
                  SoundEvents.GLASS_BREAK,
                  0.55f,
                  1.25f,
                  net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE);
          }
      }
    spawnBreathParticles(
        server, origin, look, affected.isEmpty() ? entity : affected.get(0), config);
    playBreathSound(level, entity, !affected.isEmpty());
    player.getCooldowns().addCooldown(organ.getItem(), (int)cooldown);
  }

  private static void applyColdEffect(
      LivingEntity target, CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config) {
    Level level = target.level();
    if (level.isClientSide()) {
      return;
    }
    int duration = Math.max(0, config.coldDurationTicks);
    Optional<Holder.Reference<MobEffect>> holder =
        BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
    holder.ifPresent(
        effect -> target.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true)));
    target.addEffect(
        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true, true));
    target.addEffect(
        new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 0, false, true, true));
    // 霜痕标记：供“霜痕碎裂/蒸汽灼烫”反应使用
    int frostMarkTicks =
        ChestCavity.config != null
            ? Math.max(20, ChestCavity.config.REACTION.frostMarkDurationTicks)
            : 120;
    ReactionTagOps.add(target, ReactionTagKeys.FROST_MARK, frostMarkTicks);
  }

  private static void spawnBreathParticles(
      ServerLevel server,
      Vec3 origin,
      Vec3 look,
      Entity focus,
      CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config) {
    Vec3 direction = look.normalize();
    int steps = Math.max(0, config.breathParticleSteps);
    double spacing = config.breathParticleSpacing;
    if (spacing <= 0.0D) {
      spacing = 0.1D;
    }
    for (int i = 0; i < steps; i++) {
      double scale = (i + 1) * spacing;
      Vec3 point = origin.add(direction.scale(scale));
      server.sendParticles(
          ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 6, 0.1, 0.1, 0.1, 0.02);
    }
    if (focus != null) {
      server.sendParticles(
          ParticleTypes.SNOWFLAKE,
          focus.getX(),
          focus.getY() + focus.getBbHeight() * 0.5,
          focus.getZ(),
          12,
          0.25,
          0.25,
          0.25,
          0.04);
      // 在命中点生成一小团霜雾残留域（轻控场）
      net.tigereye.chestcavity.engine.reaction.ResidueManager.spawnOrRefreshFrost(
          server,
          focus.getX(),
          focus.getY(),
          focus.getZ(),
          Math.max(0.8F, (float) (config.breathParticleSpacing * 6.0F)),
          Math.max(40, config.coldDurationTicks / 2),
          0);
    }
  }

  private static void playBreathSound(Level level, LivingEntity entity, boolean hit) {
    float volume = hit ? 0.9f : 0.6f;
    float pitch = hit ? 0.8f : 1.1f;
    level.playSound(
        null,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        SoundEvents.GENERIC_EXTINGUISH_FIRE,
        SoundSource.PLAYERS,
        volume,
        pitch);
    if (hit) {
      level.playSound(
          null,
          entity.getX(),
          entity.getY(),
          entity.getZ(),
          SoundEvents.GLASS_HIT,
          SoundSource.PLAYERS,
          0.5f,
          1.3f);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[compat/guzhenren][shuang_xi] frost breath triggered (hit={})", hit);
    }
  }
}
