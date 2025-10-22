package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.entity.MadBullEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TargetingOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCEntities;
import net.tigereye.chestcavity.registration.CCStatusEffects;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 黄骆天牛蛊（力道·肌肉）： - 被动：每5秒尝试消耗100 BASE 真元，恢复3点精力（若已满则跳过）。 -
 * 主动（ATTACKABILITY）：召唤一只“发疯的牛”直线冲锋；并给予施放者30秒“精力消耗减少”。冷却60秒，主动不可叠加。
 */
public final class HuangLuoTianNiuGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganSlowTickListener {

  public static final HuangLuoTianNiuGuOrganBehavior INSTANCE =
      new HuangLuoTianNiuGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huang_luo_tian_niu_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID; // 使用物品ID作为主动ID

  private static final String STATE_ROOT = "HuangLuoTianNiuGu";
  private static final String KEY_NEXT_PASSIVE_TICK = "NextPassiveTick";
  private static final String KEY_NEXT_READY_TICK = "NextReadyTick";
  private static final long PASSIVE_INTERVAL_TICKS = 5L * 20L;
  private static final long ACTIVE_COOLDOWN_TICKS = 60L * 20L;

  private static final double PASSIVE_BASE_ZHENYUAN_COST = 100.0D; // BASE 成本，按境界缩放
  private static final double PASSIVE_JINGLI_GAIN = 3.0D;

  // 主动附加：精力消耗减少 30s（倍率在 GuzhenrenResourceCostHelper 中读取，默认0.7）
  private static final int ACTIVE_STAMINA_REDUCE_SECONDS = 30;

  static {
    OrganActivationListeners.register(ABILITY_ID, HuangLuoTianNiuGuOrganBehavior::activateAbility);
  }

  private HuangLuoTianNiuGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || player.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    long gameTime = player.level().getGameTime();
    MultiCooldown cd = createCooldown(cc, organ);
    MultiCooldown.Entry passive = cd.entry(KEY_NEXT_PASSIVE_TICK);
    if (passive.getReadyTick() > gameTime) {
      return;
    }
    passive.setReadyAt(gameTime + PASSIVE_INTERVAL_TICKS);

    var handleOpt =
        net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    var handle = handleOpt.get();
    double cur = handle.getJingli().orElse(0.0);
    double max = handle.getMaxJingli().orElse(0.0);
    if (!(max > 0.0) || cur + 1e-4 >= max) {
      return; // 满则不执行
    }
    // 尝试按境界缩放扣100 BASE 真元，成功则+3精力（向上限夹）
    var consumed = ResourceOps.tryConsumeScaledZhenyuan(player, PASSIVE_BASE_ZHENYUAN_COST);
    if (consumed.isPresent()) {
      ResourceOps.tryAdjustJingli(player, PASSIVE_JINGLI_GAIN, true);
    }
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    if (cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    MultiCooldown cd = createCooldown(cc, organ);
    MultiCooldown.Entry nextReady = cd.entry(KEY_NEXT_READY_TICK);
    long now = player.level().getGameTime();
    if (nextReady.getReadyTick() > now) {
      return; // 冷却未就绪
    }

    // 施加“精力消耗减少”效果 30s（显示图标，关闭粒子以减少视觉干扰）
    player.addEffect(
        new MobEffectInstance(
            CCStatusEffects.HLTN_STAMINA_REDUCE,
            ACTIVE_STAMINA_REDUCE_SECONDS * 20,
            0,
            false, /*visible*/
            true, /*showIcon*/
            true));

    // 生成并发起冲锋实体（仅服务端）
    if (player.level() instanceof ServerLevel server) {
      spawnMadBull(server, player);
      // 声音提示（轻度反馈，避免“无效果”的错觉）
      server.playSound(
          null,
          player.blockPosition(),
          net.minecraft.sounds.SoundEvents.ANVIL_PLACE,
          net.minecraft.sounds.SoundSource.PLAYERS,
          0.6f,
          1.1f);
    }

    long readyAt = now + ACTIVE_COOLDOWN_TICKS;
    nextReady.setReadyAt(readyAt);
    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }
  }

  private static void spawnMadBull(ServerLevel server, Player owner) {
    var bullType = CCEntities.MAD_BULL.get();
    MadBullEntity bull = bullType.create(server);
    if (bull == null) {
      return;
    }
    // 出生在玩家前方一点点
    Vec3 eye = owner.getEyePosition();
    Vec3 look = owner.getLookAngle().normalize();
    Vec3 spawn = eye.add(look.scale(1.2)).add(0.0, -0.5, 0.0);
    bull.moveTo(spawn.x, spawn.y, spawn.z, owner.getYRot(), owner.getXRot());
    bull.setOwner(owner);

    // 锁定最近敌对目标方向（半径16），否则沿玩家朝向
    List<LivingEntity> hostiles = TargetingOps.hostilesWithinRadius(owner, server, 16.0);
    LivingEntity target = hostiles.isEmpty() ? null : hostiles.get(0);
    if (target != null) {
      bull.setChargeDirection(target.getEyePosition().subtract(spawn));
    } else {
      bull.setChargeDirection(look);
    }
    server.addFreshEntity(bull);
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(v -> Math.max(0L, v), 0L);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
