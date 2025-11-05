package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.behavior;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.JianDaoComboRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoRenShouComboTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;

/**
 * 魔道·剑道组合杀招（葬生飞剑）——框架实现：
 * - 检查冷却
 * - 以固定间隔对周围村民扣血并播放心跳音效，累计扣血总量
 * - 完成后召唤“人兽葬生”飞剑，并把累计扣血注入飞剑当前耐久
 *
 * 说明：资源/器官扣费可在下一步接入（与正道类似），当前仅搭建行为骨架与效果链。
 */
public final class JianDaoRenShouComboBehavior {

  private JianDaoRenShouComboBehavior() {}

  private static final String COOLDOWN_KEY = "JianDaoRenShouComboReadyAt";

  public static void initialize() {
    OrganActivationListeners.register(
        JianDaoComboRegistry.SKILL_ID_REN_SHOU,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            activate(player, cc);
          }
        });
  }

  private static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null || player.level().isClientSide()) return;

    // 冷却承载：使用首个满足的需求蛊（若无则静默）
    var host = findHostOrgan(cc);
    if (host == null) return;
    var state = net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState.of(host, "guzhenren:jian_dao_ren_shou_combo");
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, host).build();

    long now = player.level().getGameTime();
    if (!cooldown.entry(COOLDOWN_KEY).isReady(now)) return;

    // 选择祭献目标（周围村民，按距离排序，限额）
    if (!(player.level() instanceof ServerLevel level)) return;
    List<Villager> victims = pickVillagers(level, player.position());

    // 必须至少存在一名村民作为祭品；否则召唤失败（不进入冷却）
    if (victims.isEmpty()) {
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("需要至少一名村民作为祭品，无法施展葬生飞剑召令"));
      return;
    }

    // 成本扣除（必须成功）
    boolean paid = net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.cost
        .JianDaoRenShouComboCostOps.tryPayAndConsume(player, cc);
    if (!paid) {
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("资源或蛊虫不足，无法施展葬生飞剑召令"));
      return;
    }

    // 启动祭献过程，并设置冷却
    runSacrifice(level, player, victims, new AtomicReference<>(0.0f), new AtomicInteger(0));

    long ready = now + JianDaoRenShouComboTuning.SUMMON_COOLDOWN_TICKS;
    cooldown.entry(COOLDOWN_KEY).setReadyAt(ready);
    ComboSkillRegistry.scheduleReadyToast(player, JianDaoComboRegistry.SKILL_ID_REN_SHOU, ready, now);
  }

  private static List<Villager> pickVillagers(ServerLevel level, Vec3 center) {
    double r = JianDaoRenShouComboTuning.SACRIFICE_RADIUS;
    AABB box = new AABB(center.x - r, center.y - 2.0, center.z - r, center.x + r, center.y + 2.0, center.z + r);
    List<Villager> list = new ArrayList<>(level.getEntitiesOfClass(Villager.class, box));
    list.sort(Comparator.comparingDouble(v -> v.distanceToSqr(center)));
    if (list.size() > JianDaoRenShouComboTuning.SACRIFICE_MAX_VICTIMS) {
      return list.subList(0, JianDaoRenShouComboTuning.SACRIFICE_MAX_VICTIMS);
    }
    return list;
  }

  private static void runSacrifice(
      ServerLevel level,
      ServerPlayer player,
      List<Villager> victims,
      AtomicReference<Float> totalDrained,
      AtomicInteger pulses) {
    // 每跳：对所有存活目标扣血并播放心跳
    float drainedThisPulse = 0.0f;
    for (Villager v : victims) {
      if (v != null && v.isAlive()) {
        float hp = v.getHealth();
        if (hp > 0.0f) {
          float dmg = Math.min(JianDaoRenShouComboTuning.SACRIFICE_PULSE_DAMAGE, hp);
          v.hurt(v.damageSources().magic(), dmg);
          drainedThisPulse += dmg;
        }
      }
    }

    // 心跳音效（在玩家处播放一次）
    level.playSound(
        null,
        BlockPos.containing(player.position()),
        SoundEvents.WARDEN_HEARTBEAT,
        SoundSource.PLAYERS,
        JianDaoRenShouComboTuning.HEARTBEAT_VOLUME,
        JianDaoRenShouComboTuning.HEARTBEAT_PITCH);

    // 累加
    if (drainedThisPulse > 0.0f) {
      totalDrained.set(totalDrained.get() + drainedThisPulse);
    }

    // 继续条件：尚有存活目标且未超出脉冲上限
    boolean anyAlive = victims.stream().anyMatch(v -> v != null && v.isAlive() && v.getHealth() > 0.0f);
    int p = pulses.incrementAndGet();
    if (anyAlive && p < JianDaoRenShouComboTuning.SACRIFICE_MAX_PULSES) {
      TickOps.schedule(level, () -> runSacrifice(level, player, victims, totalDrained, pulses),
          JianDaoRenShouComboTuning.SACRIFICE_PULSE_TICKS);
    } else {
      // 结束：召唤飞剑，并注入耐久
      summon(level, player, totalDrained.get());
    }
  }

  private static void summon(ServerLevel level, ServerPlayer player, float drained) {
    // 继承：按主手物品
    ItemStack source = player.getMainHandItem();
    var cfg = new net.tigereye.chestcavity.compat.guzhenren.flyingsword.util
        .ItemAffinityUtil.Config();
    cfg.attackDamageCoef = JianDaoRenShouComboTuning.AFFINITY_ATTACK_DAMAGE_COEF;
    cfg.attackSpeedAbsCoef = JianDaoRenShouComboTuning.AFFINITY_ATTACK_SPEED_ABS_COEF;
    cfg.sharpnessDmgPerLvl = JianDaoRenShouComboTuning.AFFINITY_SHARPNESS_DMG_PER_LVL;
    cfg.sharpnessVelPerLvl = JianDaoRenShouComboTuning.AFFINITY_SHARPNESS_VEL_PER_LVL;
    cfg.unbreakingLossMultPerLvl = JianDaoRenShouComboTuning.AFFINITY_UNBREAKING_LOSS_MULT_PER_LVL;
    cfg.sweepingBase = JianDaoRenShouComboTuning.AFFINITY_SWEEPING_BASE;
    cfg.sweepingPerLvl = JianDaoRenShouComboTuning.AFFINITY_SWEEPING_PER_LVL;
    cfg.efficiencyBlockEffPerLvl = JianDaoRenShouComboTuning.AFFINITY_EFFICIENCY_BLOCK_EFF_PER_LVL;
    cfg.miningSpeedToBlockEffCoef = JianDaoRenShouComboTuning.AFFINITY_MINING_SPEED_TO_BLOCK_EFF;
    cfg.maxDamageToMaxDurabilityCoef = JianDaoRenShouComboTuning.AFFINITY_MAX_DAMAGE_TO_MAX_DURABILITY;
    cfg.armorToMaxDurabilityCoef = JianDaoRenShouComboTuning.AFFINITY_ARMOR_TO_MAX_DURABILITY;
    cfg.armorDuraLossMultPerPoint = JianDaoRenShouComboTuning.AFFINITY_ARMOR_DURA_LOSS_MULT_PER_POINT;

    var result =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.util
            .ItemAffinityUtil.evaluate(level, source, cfg);

    var sword = net.tigereye.chestcavity.compat.guzhenren.flyingsword
        .FlyingSwordSpawner.spawnFromOwnerWithModifiersAndSpec(
            level,
            player,
            source,
            net.tigereye.chestcavity.compat.guzhenren.flyingsword
                .FlyingSwordType.REN_SHOU_ZANG_SHENG,
            result.modifiers,
            result.initSpec);

    if (sword != null && drained > 0.0f) {
      // 将祭献扣血注入飞剑当前耐久（上限钳制）
      float cur = sword.getDurability();
      float injected = (float) (drained * JianDaoRenShouComboTuning.DURABILITY_INJECT_MULT);
      sword.setDurability(cur + injected);
    }
  }

  private static ItemStack findHostOrgan(ChestCavityInstance cc) {
    var priority = List.of(
        JianDaoComboRegistry.REN_SHOU_ZANG_SHENG_GU,
        JianDaoComboRegistry.JIAN_JI_GU,
        JianDaoComboRegistry.JIAN_HEN_GU,
        JianDaoComboRegistry.YU_JUN_GU,
        JianDaoComboRegistry.XIAO_HUN_GU,
        JianDaoComboRegistry.JIAN_WEN_GU);
    for (var id : priority) {
      var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(null);
      if (item == null) continue;
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack stack = cc.inventory.getItem(i);
        if (!stack.isEmpty() && stack.getItem() == item) return stack;
      }
    }
    return null;
  }
}
