package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/**
 * YuLinGu 聚合入口（facade）：
 * 后续调用方可逐步改为引用本类，以便在不破坏旧实现的情况下完成目录收口。
 */
public final class YuLinGuOps {
  private YuLinGuOps() {}

  public static boolean isPlayerMoist(Player player, OrganState state, long time) {
    return YuLinGuCalculator.isPlayerMoist(player, state, time);
  }

  public static void applyArmorBuffs(Player player, boolean hasSharkArmor) {
    YuLinGuCalculator.applyArmorBuffs(player, hasSharkArmor);
  }

  public static void drainHunger(Player player, int amount) {
    YuLinGuCalculator.drainHunger(player, amount);
  }

  public static void tickSummons(ServerLevel level, Player owner, long gameTime) {
    YuLinGuCalculator.tickSummons(level, owner, gameTime);
  }

  public static ItemStack findOrgan(Player player) {
    return YuLinGuCalculator.findYuLinGuOrgan(player);
  }

  public static boolean hasTailSynergy(ChestCavityInstance cc) {
    return YuLinGuCalculator.hasTailSynergy(cc);
  }

  public static boolean hasSharkArmor(ItemStack organ) {
    return YuLinGuCalculator.hasSharkArmor(organ);
  }

  public static void recordWetContact(Player player, ItemStack organ) {
    YuLinGuCalculator.recordWetContact(player, organ);
  }

  public static void addProgress(Player player, ChestCavityInstance cc, ItemStack organ, int amount) {
    YuLinGuCalculator.addProgress(player, cc, organ, amount);
  }

  public static boolean hasFishArmor(ItemStack organ) {
    return YuLinGuCalculator.hasFishArmor(organ);
  }

  public static int unlockedSharkTier(ItemStack organ) {
    return YuLinGuCalculator.unlockedSharkTier(organ);
  }

  public static void unlockSharkTier(ItemStack organ, int tier) {
    YuLinGuCalculator.unlockSharkTier(organ, tier);
  }

  public static void addSummon(Player owner, OwnedSharkEntity summon) {
    YuLinGuCalculator.addSummon(owner, summon);
  }

  public static List<OwnedSharkEntity> getSummons(Player owner) {
    return YuLinGuCalculator.getSummons(owner);
  }

  public static void removeSummon(Player owner, OwnedSharkEntity summon) {
    YuLinGuCalculator.removeSummon(owner, summon);
  }

  public static String stateRoot() {
    return YuLinGuTuning.STATE_ROOT;
  }
}
