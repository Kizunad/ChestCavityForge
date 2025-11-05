package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianQiaoGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordStorage;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.DespawnContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiaoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/** 剑鞘蛊（容量、被动修复、收剑令主动）。 */
public enum JianQiaoGuOrganBehavior {
  INSTANCE;

  private static final ResourceLocation ORGAN_ID = JianQiaoGuTuning.ORGAN_ID;

  private static final Component MSG_COOLDOWN = Component.literal("[剑鞘蛊] 收剑令冷却中");
  private static final Component MSG_RESOURCE_FAIL = Component.literal("[剑鞘蛊] 真元不足，术式中断");
  private static final Component MSG_NO_TARGET = Component.literal("[剑鞘蛊] 周围没有可夺取的飞剑");
  private static final Component MSG_FAIL = Component.literal("[剑鞘蛊] 收剑令未成功");
  private static final Component MSG_REPAIR_FAIL = Component.literal("[剑鞘蛊] 真元或精力不足，无法修复飞剑");

  static {
    OrganActivationListeners.register(
        JianQiaoGuTuning.ABILITY_ID, JianQiaoGuOrganBehavior::activateAbility);
  }

  public static int computeStorageCapacity(ServerPlayer player, ChestCavityInstance cc) {
    boolean equipped = hasOrganEquipped(cc);
    double daoHen =
        ResourceOps.openHandle(player)
            .map(handle -> DaoHenResourceOps.get(handle, "daohen_jiandao"))
            .orElse(0.0);
    return JianQiaoGuCalc.computeCapacity(equipped, daoHen);
  }

  public static void handleRecallRepair(
      ServerPlayer player, ChestCavityInstance cc, FlyingSwordEntity sword) {
    if (player == null || sword == null || cc == null) {
      return;
    }
    if (!hasOrganEquipped(cc)) {
      return;
    }

    double max = sword.getSwordAttributes().maxDurability;
    if (!(max > 0.0)) {
      return;
    }
    double missing = Math.max(0.0, max - sword.getDurability());
    if (!(missing > 0.0)) {
      return;
    }

    double percent = Math.min(JianQiaoGuTuning.MAX_REPAIR_PERCENT, missing / max);
    if (!(percent > 0.0)) {
      return;
    }

    double zhenyuanCost = JianQiaoGuTuning.repairZhenyuanCost(percent);
    double jingliCost = JianQiaoGuTuning.repairJingliCost(percent);
    GuzhenrenResourceCostHelper.ConsumptionResult result =
        ResourceOps.consumeStrict(player, zhenyuanCost, jingliCost);
    if (!result.succeeded()) {
      player.displayClientMessage(MSG_REPAIR_FAIL, true);
      return;
    }

    float before = sword.getDurability();
    sword.setDurability((float) Math.min(max, before + max * percent));
    float repaired = sword.getDurability() - before;
    if (repaired <= 0.0f) {
      return;
    }

    player.sendSystemMessage(
        Component.literal(
            String.format(Locale.ROOT, "[剑鞘蛊] 修复飞剑 +%.1f 耐久", repaired)));
  }

  public static boolean hasOrganEquipped(ChestCavityInstance cc) {
    return findOrgan(cc).isPresent();
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
      return;
    }
    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }

    ItemStack organ = organOpt.get();
    OrganState state = OrganState.of(organ, JianQiaoGuTuning.STATE_ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(JianQiaoGuTuning.KEY_READY_TICK).withDefault(0L);

    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    if (!ready.isReady(now)) {
      ActiveSkillRegistry.scheduleReadyToast(
          player, JianQiaoGuTuning.ABILITY_ID, ready.getReadyTick(), now);
      player.displayClientMessage(MSG_COOLDOWN, true);
      return;
    }

    long nextReady = now + JianQiaoGuTuning.ACTIVE_COOLDOWN_T;
    ready.setReadyAt(nextReady);
    ActiveSkillRegistry.scheduleReadyToast(player, JianQiaoGuTuning.ABILITY_ID, nextReady, now);

    FlyingSwordStorage storage = CCAttachments.getFlyingSwordStorage(player);
    int capacity = computeStorageCapacity(player, cc);
    if (storage.getCount() >= capacity) {
      notifyStorageFull(player, capacity);
      return;
    }

    double ourDaoHen = readDaoHen(player);
    double ourExp = readSchoolExp(player);
    Optional<CaptureCandidate> candidateOpt = selectCandidate(player, ourDaoHen, ourExp);
    if (candidateOpt.isEmpty()) {
      player.sendSystemMessage(MSG_NO_TARGET);
      return;
    }
    CaptureCandidate candidate = candidateOpt.get();

    double cost = JianQiaoGuTuning.seizeZhenyuanCost();
    GuzhenrenResourceCostHelper.ConsumptionResult costResult =
        ResourceOps.consumeStrict(player, cost, 0.0);
    if (!costResult.succeeded()) {
      player.displayClientMessage(MSG_RESOURCE_FAIL, true);
      return;
    }

    double chance = JianQiaoGuCalc.seizeChance(ourExp, candidate.exp());
    boolean success = player.getRandom().nextDouble() <= chance;
    if (!success) {
      player.sendSystemMessage(MSG_FAIL);
      candidate.owner()
          .sendSystemMessage(
              Component.literal(
                  String.format(
                      Locale.ROOT,
                      "[剑鞘蛊] %s 试图收走你的飞剑，被你阻止",
                      player.getName().getString())));
      return;
    }

    String swordName = candidate.sword().getDisplayName().getString();
    boolean captured =
        captureSword(player, cc, candidate.sword(), candidate.owner(), capacity, storage);
    if (!captured) {
      notifyStorageFull(player, capacity);
      return;
    }

    player.sendSystemMessage(
        Component.literal(
            String.format(
                Locale.ROOT,
                "[剑鞘蛊] 已收取飞剑 %s (成功率 %.0f%%)",
                swordName,
                chance * 100.0)));
    candidate.owner()
        .sendSystemMessage(
            Component.literal(
                String.format(
                    Locale.ROOT,
                    "[剑鞘蛊] %s 收走了你的飞剑",
                    player.getName().getString())));
  }

  private static Optional<CaptureCandidate> selectCandidate(
      ServerPlayer player, double ourDaoHen, double ourExp) {
    ServerLevel level = player.serverLevel();
    double range = JianQiaoGuTuning.SEIZE_RANGE;
    Vec3 centre = player.position();

    List<FlyingSwordEntity> swords =
        level.getEntitiesOfClass(
            FlyingSwordEntity.class,
            new AABB(centre, centre).inflate(range),
            sword -> isSwordEligible(sword, player));

    swords.sort(Comparator.comparingDouble(sword -> sword.distanceToSqr(player)));

    for (FlyingSwordEntity sword : swords) {
      LivingEntity ownerEntity = sword.getOwner();
      if (!(ownerEntity instanceof ServerPlayer owner) || !owner.isAlive()) {
        continue;
      }
      double ownerDaoHen = readDaoHen(owner);
      double ownerExp = readSchoolExp(owner);
      if (!JianQiaoGuCalc.canOverwhelm(ourDaoHen, ownerDaoHen, ourExp, ownerExp)) {
        continue;
      }
      return Optional.of(new CaptureCandidate(sword, owner, ownerDaoHen, ownerExp));
    }
    return Optional.empty();
  }

  private static boolean isSwordEligible(FlyingSwordEntity sword, ServerPlayer player) {
    if (sword == null || !sword.isAlive() || sword.isRemoved()) {
      return false;
    }
    if (!sword.isRecallable()) {
      return false;
    }
    LivingEntity owner = sword.getOwner();
    if (!(owner instanceof ServerPlayer ownerPlayer)) {
      return false;
    }
    if (ownerPlayer.getUUID().equals(player.getUUID())) {
      return false;
    }
    return true;
  }

  private static boolean captureSword(
      ServerPlayer captor,
      ChestCavityInstance captorCc,
      FlyingSwordEntity sword,
      ServerPlayer originalOwner,
      int capacity,
      FlyingSwordStorage storage) {
    if (!(captor.level() instanceof ServerLevel level)) {
      return false;
    }

    clearSelection(originalOwner, sword);

    sword.setOwner(captor);
    sword.setTargetEntity(null);

    handleRecallRepair(captor, captorCc, sword);

    DespawnContext ctx =
        new DespawnContext(
            sword,
            level,
            captor,
            DespawnContext.Reason.CAPTURED,
            new ItemStack(Items.IRON_SWORD));
    FlyingSwordEventRegistry.fireDespawnOrRecall(ctx);
    if (ctx.preventDespawn) {
      return false;
    }

    boolean stored = storage.recallSword(sword, capacity);
    if (!stored) {
      return false;
    }
    captor.setData(CCAttachments.FLYING_SWORD_STORAGE.get(), storage);

    sword.discard();
    return true;
  }

  private static void clearSelection(ServerPlayer owner, FlyingSwordEntity sword) {
    CCAttachments.getExistingFlyingSwordSelection(owner)
        .ifPresent(
            selection -> {
              selection
                  .getSelectedSword()
                  .ifPresent(
                      selected -> {
                        if (selected.equals(sword.getUUID())) {
                          selection.clear();
                          owner.setData(CCAttachments.FLYING_SWORD_SELECTION.get(), selection);
                        }
                      });
            });
  }

  private static double readDaoHen(ServerPlayer player) {
    return ResourceOps.openHandle(player)
        .map(handle -> DaoHenResourceOps.get(handle, "daohen_jiandao"))
        .orElse(0.0);
  }

  private static double readSchoolExp(ServerPlayer player) {
    return ResourceOps.openHandle(player)
        .map(handle -> handle.read("liupai_jiandao").orElse(0.0))
        .orElse(0.0);
  }

  private static void notifyStorageFull(ServerPlayer player, int capacity) {
    player.sendSystemMessage(
        Component.literal(
            String.format(Locale.ROOT, "[剑鞘蛊] 剑鞘已满 (最多%d把)", Math.max(0, capacity))));
  }

  private static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }
    Item organItem = getOrganItem();
    if (organItem == null) {
      return Optional.empty();
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == organItem) {
        return Optional.of(stack);
      }
    }
    return Optional.empty();
  }

  private static Item getOrganItem() {
    return BuiltInRegistries.ITEM.get(ORGAN_ID);
  }

  private record CaptureCandidate(
      FlyingSwordEntity sword, ServerPlayer owner, double daoHen, double exp) {}
}
