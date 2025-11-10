package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.event.NoDropEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuShiSummonTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu.YuShiSummonCalculator;

/** [主动C] 饵祭召鲨：消耗对应蛊材召唤鲨鱼同伴，最多同时存在五条。 */
public final class YuShiSummonSharkSkill {

  public static final ResourceLocation ABILITY_ID = YuShiSummonTuning.ABILITY_ID;

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");

  private static final String COOLDOWN_KEY = YuShiSummonTuning.COOLDOWN_KEY;
  private static final int COOLDOWN_TICKS = YuShiSummonTuning.COOLDOWN_TICKS;
  private static final double ZHENYUAN_COST = YuShiSummonTuning.ZHENYUAN_COST;
  private static final double JINGLI_COST = YuShiSummonTuning.JINGLI_COST;
  private static final int HUNGER_COST = YuShiSummonTuning.HUNGER_COST;
  private static final int TTL_TICKS = YuShiSummonTuning.TTL_TICKS;
  private static final int OFFERING_COST = YuShiSummonTuning.OFFERING_COST;

  /*
   *  "item.guzhenren.sha_yu_ya_chi": "蛊材_冰鲛鲨牙齿",
   *  "item.guzhenren.sha_yu_ya_chi_1": "蛊材_冰鳞鲨牙齿",
   *  "item.guzhenren.sha_yu_ya_chi_2": "蛊材_玄霜鲛鲨牙齿",
   *  "item.guzhenren.sha_yu_ya_chi_3": "蛊材_寒渊冰鲨牙齿",
   *  "item.guzhenren.sha_yu_ya_chi_4": "蛊材_冰魄龙纹鲨牙齿",
   *  "item.guzhenren.sha_yu_yu_chi": "蛊材_冰鲛鲨鱼翅",
   *  "item.guzhenren.sha_yu_yu_chi_1": "蛊材_冰鳞鲨鱼翅",
   *  "item.guzhenren.sha_yu_yu_chi_2": "蛊材_玄霜鲛鲨鱼翅",
   *  "item.guzhenren.sha_yu_yu_chi_3": "蛊材_寒渊冰鲨鱼翅",
   *  "item.guzhenren.sha_yu_yu_chi_4": "蛊材_冰魄龙纹鲨鱼翅",
   */
  // YuShiSummonSharkSkill 内新增：一个极小的兜底表（仅当标签失效时使用）
  private static final Map<ResourceLocation, Integer> FALLBACK_MATERIAL_TIERS =
      Map.ofEntries(
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi"), 1),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi"), 1),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_1"), 2),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_1"), 2),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_2"), 3),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_2"), 3),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_3"), 4),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_3"), 4),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_4"), 5),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_4"), 5));

  private static final TagKey<Item> MATERIALS_TAG = YuShiSummonTuning.MATERIALS_TAG;
  private static final TagKey<Item> TIER1_TAG = YuShiSummonTuning.TIER1_TAG;
  private static final TagKey<Item> TIER2_TAG = YuShiSummonTuning.TIER2_TAG;
  private static final TagKey<Item> TIER3_TAG = YuShiSummonTuning.TIER3_TAG;
  private static final TagKey<Item> TIER4_TAG = YuShiSummonTuning.TIER4_TAG;
  private static final TagKey<Item> TIER5_TAG = YuShiSummonTuning.TIER5_TAG;

  // 按蛊材阶数索引召唤的实体 ID（0 位保留为空以便直接按阶数取值）。
  private static final ResourceLocation[] TIER_ENTITY_IDS = YuShiSummonTuning.TIER_ENTITY_IDS;

  // 玩家背包结构中副手对应的槽位索引（Vanilla 固定为 40）。
  private static final int OFFHAND_SLOT = YuShiSummonTuning.OFFHAND_SLOT;

  private YuShiSummonSharkSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(ABILITY_ID, YuShiSummonSharkSkill::activate);
  }

  public static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, "YuLinGu");
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(COOLDOWN_KEY).withDefault(0L);
    long now = serverLevel.getGameTime();
    if (!ready.isReady(now)) {
      return;
    }

    Inventory inventory = player.getInventory();
    Optional<YuShiSummonCalculator.OfferingSlot> offeringOpt =
        YuShiSummonCalculator.findBestOffering(inventory);
    if (offeringOpt.isEmpty()) {
      sendFailure(
          player, "你大口吞下了空气。鱼鳞蛊沉默以对。(也许需要整组对应阶的鲨材作为供品...)");
      return;
    }
    YuShiSummonCalculator.OfferingSlot offering = offeringOpt.get();
    ItemStack offeringStack = offering.stack();
    int materialTier = YuShiSummonCalculator.tierOf(offeringStack);

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      sendFailure(player, "真元不足，召鲨仪式失败。");
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      sendFailure(player, "精力不足，召鲨仪式失败。");
      return;
    }
    player
        .getFoodData()
        .setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - HUNGER_COST));

    YuShiSummonCalculator.consumeOffering(player, inventory, offering, OFFERING_COST);

    int unlocked = YuLinGuOps.unlockedSharkTier(organ);
    if (materialTier > unlocked) {
      YuLinGuOps.unlockSharkTier(organ, materialTier);
      unlocked = materialTier;
    }
    int actualTier = Math.max(materialTier, unlocked);

    YuShiSummonCalculator.manageSummonLimit(player, actualTier, serverLevel);

    EntityType<?> type = YuShiSummonCalculator.resolveEntityType(actualTier);
    if (type == null) {
      return;
    }
    Vec3 spawnPos = YuShiSummonCalculator.computeSpawnPos(player);
    Entity spawned = type.create(serverLevel);
    if (!(spawned instanceof LivingEntity living)) {
      return;
    }
    living.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
    serverLevel.addFreshEntity(living);
    // 标记召唤物为“无掉落”并关闭拾取，避免召唤鲨鱼提供额外战利品。
    YuShiSummonCalculator.finalizeSpawned(serverLevel, player, living);

    OwnedSharkEntity tracked =
        new OwnedSharkEntity(living.getUUID(), player.getUUID(), actualTier, now, now + TTL_TICKS);
    YuLinGuOps.addSummon(player, tracked);
    YuLinGuOps.recordWetContact(player, organ);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);

    serverLevel.playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 1.0f, 0.9f);
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
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
  }

  private record OfferingSlot(int slot, ItemStack stack, InteractionHand hand) {}
}
