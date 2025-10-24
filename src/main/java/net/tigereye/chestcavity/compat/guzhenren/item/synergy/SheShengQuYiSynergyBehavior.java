package net.tigereye.chestcavity.compat.guzhenren.item.synergy;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 舍生取义（器官联动）： - 条件：胸腔同时存在“舍利蛊（任意品阶）”与“生机叶/九叶生机草”。 - 被动：道德 → 攻击伤害加成（可配置缩放与上限）；移除任一器官后加成清除。 -
 * 主动：按键触发“誓约”，短时间降低上限以换取大量道德（消耗寿元），到期自动回滚。
 */
public final class SheShengQuYiSynergyBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener {

  public static final SheShengQuYiSynergyBehavior INSTANCE = new SheShengQuYiSynergyBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation SHENG_JI_YE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "sheng_ji_xie");
  private static final ResourceLocation JIU_YE_SHENG_JI_CAO_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_xie_sheng_ji_cao");

  private static final ResourceLocation QING_TONG_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_tong_she_li_gu");
  private static final ResourceLocation CHI_TIE_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "chi_tie_she_li_gu");
  private static final ResourceLocation BAI_YIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yin_she_li_gu");
  private static final ResourceLocation HUANG_JIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huang_jin_she_li_gu");
  private static final ResourceLocation ZI_JIN_SHE_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_jin_she_li_gu");

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "synergy/she_sheng_qu_yi");
  private static final String STATE_ROOT = "SheShengQuYi";
  private static final String KEY_VOW_READY = "VowReadyAt";
  private static final String KEY_VOW_EXPIRE = "VowExpireAt";
  private static final String KEY_JINGLI_DELTA = "VowJingliDelta"; // 记录本次临时上限变更量（负数）

  // 被动参数（可通过 JVM system properties 覆盖）
  private static final double MORAL_TO_DAMAGE_FACTOR =
      getDoubleProp("chestcavity.synergy.moralFactor", 0.05);
  private static final double MORAL_TO_DAMAGE_CAP =
      getDoubleProp("chestcavity.synergy.moralCap", 10.0);
  private static final boolean PASSIVE_ENABLED =
      getBooleanProp("chestcavity.synergy.moralPassive", true);

  // 主动参数（可配置）
  private static final int VOW_DURATION_TICKS =
      (int) getDoubleProp("chestcavity.synergy.vowDurationTicks", 30 * 20);
  private static final int VOW_COOLDOWN_TICKS =
      (int) getDoubleProp("chestcavity.synergy.vowCooldownTicks", 120 * 20);
  private static final double JINGLI_CAP_DELTA = -50.0; // 直接修改最大精力 -50
  private static final double HP_MAX_MULTIPLIER = -0.5; // 生命上限 -50%（作为 MULTIPLY_TOTAL）

  private static final ResourceLocation MORAL_ATTACK_ID =
      ResourceLocation.fromNamespaceAndPath("chestcavity", "synergy/moral_attack");
  private static final ResourceLocation VOW_HP_ID =
      ResourceLocation.fromNamespaceAndPath("chestcavity", "synergy/vow_hp");

  static {
    OrganActivationListeners.register(ABILITY_ID, SheShengQuYiSynergyBehavior::activateAbility);
  }

  private SheShengQuYiSynergyBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)) {
      return;
    }
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel) || level.isClientSide() || !player.isAlive()) {
      return;
    }
    // 仅在“锚定器官”（优先九叶，否则生机叶）上执行一次，避免重复
    ItemStack anchor = findAnchorOrgan(cc);
    if (anchor.isEmpty() || !ItemStack.isSameItemSameComponents(anchor, organ)) {
      return;
    }

    OrganState anchorState = organState(anchor, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, anchor, anchorState);
    long now = serverLevel.getGameTime();

    int synergyTier = resolveSynergyTier(cc); // 0=无；1=生机叶；2/3=九叶当前阶
    boolean hasSynergy = synergyTier > 0 && hasAnySheLi(cc);

    // 被动：道德 → 攻击伤害（临时修饰符）
    if (PASSIVE_ENABLED) {
      applyMoralPassive(player, hasSynergy);
    }

    // 处理誓约到期回滚
    long expireAt = Math.max(0L, anchorState.getLong(KEY_VOW_EXPIRE, 0L));
    if (expireAt > 0L && now >= expireAt) {
      revertVow(player, anchorState);
      anchorState.setLong(KEY_VOW_EXPIRE, 0L, v -> Math.max(0L, v), 0L);
      sendSlotUpdate(cc, anchor);
    }
  }

  private static void applyMoralPassive(Player player, boolean enabled) {
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack == null) {
      return;
    }
    if (!enabled) {
      AttributeOps.removeById(attack, MORAL_ATTACK_ID);
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      AttributeOps.removeById(attack, MORAL_ATTACK_ID);
      return;
    }
    double moral = handleOpt.get().getDaode().orElse(0.0);
    double bonus = MORAL_TO_DAMAGE_FACTOR * moral;
    if (MORAL_TO_DAMAGE_CAP > 0.0) {
      bonus = Math.min(bonus, MORAL_TO_DAMAGE_CAP);
    }
    if (!Double.isFinite(bonus) || bonus == 0.0) {
      AttributeOps.removeById(attack, MORAL_ATTACK_ID);
      return;
    }
    AttributeOps.replaceTransient(
        attack,
        MORAL_ATTACK_ID,
        new AttributeModifier(MORAL_ATTACK_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || !player.isAlive()) {
      return;
    }
    ItemStack anchor = findAnchorOrgan(cc);
    if (anchor.isEmpty()) {
      return;
    }
    int synergyTier = resolveSynergyTier(cc);
    if (synergyTier <= 0 || !hasAnySheLi(cc)) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("需要舍利蛊与生机系器官"), true);
      return;
    }

    ServerLevel level = player.serverLevel();
    long now = level.getGameTime();
    OrganState anchorState = INSTANCE.organState(anchor, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, anchor, anchorState);
    MultiCooldown.Entry entry = cooldown.entry(KEY_VOW_READY);
    if (!entry.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    double zhuanshu = Math.max(1.0, handle.getZhuanshu().orElse(1.0));
    double daodeGain = zhuanshu * 10.0 * synergyTier;

    // 寿元代价：生机叶 1.0 年；九叶 0.5 年
    double shouyuanCost = hasJiuYe(cc) ? 0.5 : 1.0;
    if (handle.getShouyuan().orElse(0.0) + 1.0E-4D < shouyuanCost) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.literal("寿元不足，无法施行誓约"), true);
      return;
    }

    // 应用资源变更
    ResourceOps.tryAdjustDouble(handle, "shouyuan", -shouyuanCost, true, null);
    ResourceOps.tryAdjustDouble(handle, "daode", daodeGain, true, null);

    // 上限削减：精力 -50，生命上限 -50%（属性修饰）
    ResourceOps.tryAdjustDouble(handle, "zuida_jingli", JINGLI_CAP_DELTA, true, "zuida_jingli");
    anchorState.setDouble(KEY_JINGLI_DELTA, JINGLI_CAP_DELTA, v -> v, 0.0);
    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      AttributeOps.replaceTransient(
          maxHealth,
          VOW_HP_ID,
          new AttributeModifier(
              VOW_HP_ID, HP_MAX_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      // clamp 当前生命到新上限内
      double newMax = maxHealth.getValue();
      if (player.getHealth() > (float) newMax) {
        player.setHealth((float) Math.max(1.0, newMax));
      }
    }

    long expire = now + VOW_DURATION_TICKS;
    anchorState.setLong(KEY_VOW_EXPIRE, expire, v -> Math.max(0L, v), 0L);

    long readyAt = now + VOW_COOLDOWN_TICKS;
    entry.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);

    player.displayClientMessage(
        net.minecraft.network.chat.Component.literal("舍生取义已施行（上限临时下降）"), true);
    INSTANCE.sendSlotUpdate(cc, anchor);
  }

  private static void revertVow(Player player, OrganState anchorState) {
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    handleOpt.ifPresent(
        handle -> {
          double delta = anchorState.getDouble(KEY_JINGLI_DELTA, 0.0);
          if (delta != 0.0) {
            ResourceOps.tryAdjustDouble(handle, "zuida_jingli", -delta, true, "zuida_jingli");
          }
        });
    AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      AttributeOps.removeById(maxHealth, VOW_HP_ID);
      // 回滚后再 clamp 一次
      double newMax = maxHealth.getValue();
      if (player.getHealth() > (float) newMax) {
        player.setHealth((float) newMax);
      }
    }
    anchorState.setDouble(KEY_JINGLI_DELTA, 0.0, v -> v, 0.0);
    player.displayClientMessage(net.minecraft.network.chat.Component.literal("誓约已结束，上限恢复"), true);
  }

  private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    return MultiCooldown.builder(state)
        .withSync(cc, organ)
        .withLongClamp(v -> Math.max(0L, v), 0L)
        .build();
  }

  private static ItemStack findAnchorOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    // 优先九叶，其次生机叶
    Item jiuye =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(JIU_YE_SHENG_JI_CAO_ID)
            .orElse(null);
    Item shengji =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(SHENG_JI_YE_ID)
            .orElse(null);
    ItemStack fallback = ItemStack.EMPTY;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      if (jiuye != null && s.getItem() == jiuye) return s;
      if (fallback.isEmpty() && shengji != null && s.getItem() == shengji) fallback = s;
    }
    return fallback;
  }

  private static boolean hasAnySheLi(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    Item qt =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(QING_TONG_SHE_LI_GU_ID)
            .orElse(null);
    Item ct =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(CHI_TIE_SHE_LI_GU_ID)
            .orElse(null);
    Item by =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(BAI_YIN_SHE_LI_GU_ID)
            .orElse(null);
    Item hj =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(HUANG_JIN_SHE_LI_GU_ID)
            .orElse(null);
    Item zj =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(ZI_JIN_SHE_LI_GU_ID)
            .orElse(null);
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      Item it = s.getItem();
      if (it == qt || it == ct || it == by || it == hj || it == zj) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasJiuYe(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) return false;
    Item jiuye =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(JIU_YE_SHENG_JI_CAO_ID)
            .orElse(null);
    if (jiuye == null) return false;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (!s.isEmpty() && s.getItem() == jiuye) return true;
    }
    return false;
  }

  private static int resolveSynergyTier(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return 0;
    }
    Item jiuye =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(JIU_YE_SHENG_JI_CAO_ID)
            .orElse(null);
    Item shengji =
        net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(SHENG_JI_YE_ID)
            .orElse(null);
    int shengjiTier = 0;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      if (jiuye != null && s.getItem() == jiuye) {
        OrganState state = INSTANCE.organState(s, "JiuYeShengJiCao");
        int tier = Mth.clamp(state.getInt("Tier", 1), 1, 3);
        return tier;
      }
      if (shengji != null && s.getItem() == shengji) {
        shengjiTier = Math.max(shengjiTier, 1);
      }
    }
    return shengjiTier; // 1=生机叶，0=未找到
  }

  private static boolean getBooleanProp(String key, boolean def) {
    try {
      String v = System.getProperty(key);
      if (v == null) return def;
      return Boolean.parseBoolean(v);
    } catch (Throwable t) {
      return def;
    }
  }

  private static double getDoubleProp(String key, double def) {
    try {
      String v = System.getProperty(key);
      if (v == null) return def;
      return Double.parseDouble(v);
    } catch (Throwable t) {
      return def;
    }
  }
}
