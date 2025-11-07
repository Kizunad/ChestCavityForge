package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.hooks;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.HitEntityContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.SwordShadowRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * 飞剑命中时，有概率触发“剑影蛊分身”（飞剑版）：
 * - 触发条件：飞剑的胸腔中包含 guzhenren:jian_ying_gu
 * - 触发概率：JianYingTuning.PASSIVE_TRIGGER_CHANCE
 * - 分身伤害：以本次飞剑命中伤害为基础，叠加 Owner 的剑道道痕与流派经验（不扣资源）
 * - 分身存在时间：同样按 Owner 的道痕与流派经验增加
 */
public final class JianYingGuShadowHook implements FlyingSwordEventHook {

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.parse("guzhenren:jian_ying_gu");

  @Override
  public void onHitEntity(HitEntityContext ctx) {
    if (ctx == null || ctx.sword == null || ctx.level == null) {
      return;
    }
    // 检查飞剑胸腔是否装有剑影蛊
    ChestCavityInstance cc =
        ChestCavityEntity.of(ctx.sword).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null || cc.inventory == null) {
      return;
    }
    int organCount = countOrgans(cc);
    if (organCount <= 0) {
      return;
    }
    // 几率触发
    if (ctx.sword.getRandom().nextDouble() >= JianYingTuning.PASSIVE_TRIGGER_CHANCE) {
      return;
    }

    // 基础伤害：本次飞剑命中的伤害（速度²计算后）
    double baseDamage = Math.max(0.0, ctx.damage);

    // 资源所有者：若 Owner 是玩家则提供，以读取道痕/流派经验（不扣资源）
    ServerPlayer resourceOwner = (ctx.owner instanceof ServerPlayer sp) ? sp : null;

    SwordShadowRuntime.activateCloneForEntityWithBaseDamage(
        ctx.sword, resourceOwner, cc, organCount, false, baseDamage);
  }

  private static int countOrgans(ChestCavityInstance cc) {
    int total = 0;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        total += Math.max(1, stack.getCount());
      }
    }
    return total;
  }
}

