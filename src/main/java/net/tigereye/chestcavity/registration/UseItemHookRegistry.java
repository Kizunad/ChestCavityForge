package net.tigereye.chestcavity.registration;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.effects.EffectContext;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

/**
 * 统一的物品使用监听桥：Start/Finish/Abort 三相分派到 SkillEffectBus。
 *
 * <p>生成的 hookId 规范：
 * - 通用：chestcavity:use_item
 * - 指定物品：chestcavity:use_item/item/<ns>/<path>
 * - 指定标签：chestcavity:use_item/tag/<ns>/<path>
 *
 * 效果编写方可用正则匹配上述 ID 快速批量挂接（例如 ^chestcavity:use_item/tag/guzhenren/ying_dao$）。
 */
public final class UseItemHookRegistry {

  private UseItemHookRegistry() {}

  private static volatile boolean registered = false;

  public static void register() {
    if (registered) return;
    registered = true;
    NeoForge.EVENT_BUS.addListener(UseItemHookRegistry::onItemUseStart);
    NeoForge.EVENT_BUS.addListener(UseItemHookRegistry::onItemUseFinish);
    NeoForge.EVENT_BUS.addListener(UseItemHookRegistry::onItemUseAbort);
  }

  private static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (player.level().isClientSide()) return;
    ItemStack stack = event.getItem();
    if (stack == null || stack.isEmpty()) return;

    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              EffectContext.UseItemInfo info = EffectContext.UseItemInfo.ofStart(stack, event.getDuration());
              for (ResourceLocation hookId : computeHookIds(stack)) {
                SkillEffectBus.pre(player, hookId, cc, null, info);
              }
            });
  }

  private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (player.level().isClientSide()) return;
    ItemStack stack = event.getItem();
    if (stack == null || stack.isEmpty()) return;

    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              EffectContext.UseItemInfo info = EffectContext.UseItemInfo.ofFinish(stack);
              for (ResourceLocation hookId : computeHookIds(stack)) {
                SkillEffectBus.post(
                    player,
                    hookId,
                    cc,
                    null,
                    ActiveSkillRegistry.TriggerResult.SUCCESS,
                    info);
              }
            });
  }

  private static void onItemUseAbort(LivingEntityUseItemEvent.Stop event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (player.level().isClientSide()) return;
    ItemStack stack = event.getItem();
    if (stack == null || stack.isEmpty()) return;

    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              EffectContext.UseItemInfo info = EffectContext.UseItemInfo.ofAbort(stack);
              for (ResourceLocation hookId : computeHookIds(stack)) {
                SkillEffectBus.post(
                    player,
                    hookId,
                    cc,
                    null,
                    ActiveSkillRegistry.TriggerResult.BLOCKED_BY_HANDLER,
                    info);
              }
            });
  }

  private static List<ResourceLocation> computeHookIds(ItemStack stack) {
    List<ResourceLocation> ids = new ArrayList<>();
    // 通用：匹配所有物品使用
    ids.add(ChestCavity.id("use_item"));

    // 物品 ID
    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    ids.add(ChestCavity.id("use_item/item/" + itemId.getNamespace() + "/" + itemId.getPath()))
        ;

    // 物品标签（全部枚举），便于基于 guzhenren:* 等标签批量挂接
    Holder<Item> holder = stack.getItem().builtInRegistryHolder();
    for (TagKey<Item> tag : holder.tags().toList()) {
      ResourceLocation t = tag.location();
      ids.add(ChestCavity.id("use_item/tag/" + t.getNamespace() + "/" + t.getPath()));
    }
    return ids;
  }
}
