package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.SoundOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import org.joml.Vector3f;

/**
 * 紫金舍利蛊（五转）： - 被动：每120秒生成“舍利光域”，扣 5000 基础真元 + 20 魂魄 + 20 精力，为友方赋予 再生V+抗性I 持续10秒，并释放粒子/音效 -
 * 主动：涅槃舍利：立即燃烧当前 50% 生命与真元；生成 15s 领域，领域内玩家友方每秒回复各自最大 10% 的真元/魂魄/精力/生命；冷却 5 分钟
 */
public enum ZaijinSheLiGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_jin_she_li_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID;

  private static final String STATE_ROOT = "ZiJinSheLiGu";
  private static final String KEY_PASSIVE_READY_AT = "PassiveReadyAt";
  private static final String KEY_ACTIVE_COOLDOWN_UNTIL = "ActiveCooldownUntil";

  private static final long PASSIVE_INTERVAL_TICKS = 120L * 20L; // 120s
  private static final long ACTIVE_DURATION_TICKS = 15L * 20L; // 15s
  private static final long ACTIVE_COOLDOWN_TICKS = 5L * 60L * 20L; // 5min

  private static final int PASSIVE_BUFF_DURATION_TICKS = 10 * 20;
  private static final int REGEN_AMP = 4; // 再生V -> amp=4
  private static final int RESIST_AMP = 0; // 抗性I -> amp=0
  private static final double DOMAIN_RADIUS = 6.0;

  static {
    OrganActivationListeners.register(ABILITY_ID, ZaijinSheLiGuOrganBehavior::activateAbility);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null
        || entity.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) return;
    if (!matchesOrgan(organ)) return;
    ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
    if (server == null) return;

    MultiCooldown cd = createCooldown(cc, organ);
    MultiCooldown.Entry passiveReady = cd.entry(KEY_PASSIVE_READY_AT);
    long now = server.getGameTime();
    if (passiveReady.getReadyTick() <= 0L) {
      passiveReady.setReadyAt(now + PASSIVE_INTERVAL_TICKS);
    }
    passiveReady.onReady(
        server,
        now,
        () -> {
          // 尝试支付资源：5000 基础真元 + 20 魂魄 + 20 精力
          if (!consumePassiveCost(entity)) {
            // 未支付成功，顺延下一次
            MultiCooldown.Entry e = createCooldown(cc, organ).entry(KEY_PASSIVE_READY_AT);
            e.setReadyAt(server.getGameTime() + PASSIVE_INTERVAL_TICKS);
            e.onReady(server, server.getGameTime(), () -> {});
            return;
          }
          // 施加友方buff与FX
          applyPassiveDomain(server, entity);
          MultiCooldown.Entry e = createCooldown(cc, organ).entry(KEY_PASSIVE_READY_AT);
          e.setReadyAt(server.getGameTime() + PASSIVE_INTERVAL_TICKS);
          e.onReady(server, server.getGameTime(), () -> {});
        });
  }

  public static void activateAbility(LivingEntity caster, ChestCavityInstance cc) {
    if (caster == null || cc == null || caster.level().isClientSide()) return;
    ItemStack organ = findPrimaryOrgan(cc);
    if (organ.isEmpty()) return;
    ServerLevel server = caster.level() instanceof ServerLevel s ? s : null;
    if (server == null) return;

    MultiCooldown cd = createCooldown(cc, organ);
    long now = server.getGameTime();
    long cdUntil = cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).getReadyTick();
    if (now < Math.max(0L, cdUntil)) return;

    // 燃烧当前 50% 生命与真元
    burnHalfLifeAndZhenyuan(caster);

    // 15秒领域：每秒对玩家友方脉冲10%回复
    final int pulses = (int) (ACTIVE_DURATION_TICKS / 20L);
    for (int i = 1; i <= pulses; i++) {
      int delay = i * 20;
      TickOps.schedule(server, () -> pulseNirvanaDomain(server, caster), delay);
    }
    // 视觉/音效（启动时播放一次基础FX）
    playNirvanaFxStart(server, caster);
    // 冷却
    long readyAt = now + ACTIVE_COOLDOWN_TICKS;
    cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).setReadyAt(readyAt);
    if (caster instanceof net.minecraft.server.level.ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }
  }

  private static boolean consumePassiveCost(LivingEntity entity) {
    // 5000 基础真元 + 20 魂魄 + 20 精力
    var zrjl = ResourceOps.consumeStrict(entity, 5000.0, 0.0);
    if (!zrjl.succeeded()) return false;
    var hp = ResourceOps.consumeHunpoStrict(entity, 20.0);
    if (!hp.succeeded()) {
      if (entity instanceof Player p) ResourceOps.refund(p, zrjl);
      return false;
    }
    // 精力-20
    ResourceOps.tryAdjustJingli(entity, -20.0, true);
    return true;
  }

  private static void applyPassiveDomain(ServerLevel server, LivingEntity caster) {
    // 友方：再生V + 抗性I 10s
    List<LivingEntity> allies =
        server.getEntitiesOfClass(
            LivingEntity.class,
            caster.getBoundingBox().inflate(DOMAIN_RADIUS),
            t -> t != null && t.isAlive() && t != caster && t.isAlliedTo(caster));
    for (LivingEntity ally : allies) {
      ally.addEffect(
          new MobEffectInstance(
              MobEffects.REGENERATION, PASSIVE_BUFF_DURATION_TICKS, REGEN_AMP, false, true, true));
      ally.addEffect(
          new MobEffectInstance(
              MobEffects.DAMAGE_RESISTANCE,
              PASSIVE_BUFF_DURATION_TICKS,
              RESIST_AMP,
              false,
              true,
              true));
    }
    playPassiveDomainFx(server, caster);
  }

  private static void burnHalfLifeAndZhenyuan(LivingEntity caster) {
    // 生命：按当前生命的一半
    float current = caster.getHealth();
    float healthCost = Math.max(0.0f, current * 0.5f);
    if (healthCost > 0.0f) {
      ResourceOps.drainHealth(caster, healthCost);
    }
    // 真元：按当前真元的一半
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(caster);
    if (handleOpt.isPresent()) {
      var handle = handleOpt.get();
      double cur = handle.read("zhenyuan").orElse(0.0);
      if (cur > 0.0) {
        ResourceOps.tryAdjustZhenyuan(handle, -cur * 0.5, true);
      }
    }
  }

  private static void pulseNirvanaDomain(ServerLevel server, LivingEntity caster) {
    AABB area = caster.getBoundingBox().inflate(DOMAIN_RADIUS);
    List<Player> players =
        server.getEntitiesOfClass(
            Player.class, area, p -> p != null && p.isAlive() && p.isAlliedTo(caster));
    for (Player player : players) {
      Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
          GuzhenrenResourceBridge.open(player);
      if (handleOpt.isEmpty()) continue;
      GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
      // 每秒回复最大10%
      double maxZr = handle.read("zuida_zhenyuan").orElse(0.0);
      double maxHp = player.getMaxHealth();
      double maxHunpo = handle.read("zuida_hunpo").orElse(0.0);
      double maxJingli = handle.read("zuida_jingli").orElse(0.0);
      if (maxZr > 0.0) ResourceOps.tryReplenishScaledZhenyuan(player, maxZr * 0.10, true);
      if (maxHunpo > 0.0) handle.adjustDouble("hunpo", maxHunpo * 0.10, true, "zuida_hunpo");
      if (maxJingli > 0.0) ResourceOps.tryAdjustJingli(handle, maxJingli * 0.10, true);
      if (maxHp > 0.0) player.heal((float) (maxHp * 0.10));
    }
    playNirvanaFxPulse(server, caster);
  }

  private static void playPassiveDomainFx(ServerLevel server, LivingEntity caster) {
    // 核心层：金色莲状旋转光轮（END_ROD 螺旋上升）
    Vec3 center = caster.position();
    double baseY = caster.getY();
    for (int i = 0; i < 10; i++) {
      double angle = (server.getGameTime() + i * 4) * 0.3;
      double radius = 0.8 + (i % 3) * 0.2;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      double y = baseY + 0.1 + (i * 0.04);
      server.sendParticles(ParticleTypes.END_ROD, x, y, z, 4, 0.01, 0.0, 0.01, 0.0);
    }
    // 外环层：扩散光晕（GLOW 环）
    playExpandingRing(server, caster, 1.0, 6.0, 24);
    // 灵魂升腾层：SOUL_FIRE_FLAME 随机上升
    for (int i = 0; i < 10; i++) {
      double ang = caster.getRandom().nextDouble() * Math.PI * 2;
      double r = caster.getRandom().nextDouble() * DOMAIN_RADIUS;
      double x = center.x + Math.cos(ang) * r;
      double z = center.z + Math.sin(ang) * r;
      double vy = 0.06 + caster.getRandom().nextDouble() * 0.06;
      server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, baseY + 0.1, z, 1, 0.0, vy, 0.0, 0.0);
    }
    // 音效
    server.playSound(
        null,
        caster.getX(),
        caster.getY(),
        caster.getZ(),
        SoundEvents.BEACON_ACTIVATE,
        SoundSource.PLAYERS,
        0.9f,
        1.1f);
    // 自定义“咚咚”提示音
    SoundOps.play(
        server,
        caster.position(),
        ResourceLocation.parse("chestcavity:custom.common.dong_dong"),
        SoundSource.PLAYERS,
        0.8f,
        1.0f);
  }

  private static void playNirvanaFxStart(ServerLevel server, LivingEntity caster) {
    // 启动时：核心环+初始脉冲
    playPassiveDomainFx(server, caster);
    playNirvanaFxPulse(server, caster);
  }

  private static void playNirvanaFxPulse(ServerLevel server, LivingEntity caster) {
    // 辐射层：DUST_COLOR_TRANSITION 放射脉冲（快速扩散）
    DustColorTransitionOptions dust =
        new DustColorTransitionOptions(
            new Vector3f(1.0f, 0.95f, 0.71f), new Vector3f(1.0f, 1.0f, 1.0f), 2.0f);
    Vec3 c = caster.position();
    int segments = 48;
    double y = caster.getY() + 0.1;
    for (int i = 0; i < segments; i++) {
      double angle = (Math.PI * 2 * i) / segments;
      for (double r = 0; r <= DOMAIN_RADIUS; r += DOMAIN_RADIUS / 10.0) {
        double x = c.x + Math.cos(angle) * r;
        double z = c.z + Math.sin(angle) * r;
        server.sendParticles(dust, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
    }
    server.playSound(
        null,
        caster.getX(),
        caster.getY(),
        caster.getZ(),
        SoundEvents.GLOW_ITEM_FRAME_ADD_ITEM,
        SoundSource.PLAYERS,
        0.6f,
        1.3f);
  }

  private static void playExpandingRing(
      ServerLevel server, LivingEntity caster, double rStart, double rEnd, int points) {
    Vec3 c = caster.position();
    double y = caster.getY() + 0.1;
    for (double r = rStart; r <= rEnd; r += 1.0) {
      for (int i = 0; i < points; i++) {
        double angle = (Math.PI * 2 * i) / points;
        double x = c.x + Math.cos(angle) * r;
        double z = c.z + Math.sin(angle) * r;
        server.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0.02, 0.0, 0.02, 0.0);
      }
    }
    server.playSound(
        null,
        caster.getX(),
        caster.getY(),
        caster.getZ(),
        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS,
        SoundSource.PLAYERS,
        0.4f,
        1.0f);
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return ORGAN_ID.equals(id);
  }

  private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) return ItemStack.EMPTY;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (matchesOrgan(candidate)) return candidate;
    }
    return ItemStack.EMPTY;
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder b =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L);
    if (cc != null) b.withSync(cc, organ);
    else b.withOrgan(organ);
    return b.build();
  }
}
