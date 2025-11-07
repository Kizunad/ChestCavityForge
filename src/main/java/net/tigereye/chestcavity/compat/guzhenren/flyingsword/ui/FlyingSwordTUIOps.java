package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordStorage;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordModelTuning;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemDurabilityUtil;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.util.ItemIdentityUtil;

/**
 * 供 TUI/网络调用的后端操作。
 */
public final class FlyingSwordTUIOps {
  private FlyingSwordTUIOps() {}

  private static HolderLookup.Provider provider(ServerLevel level) {
    return level.registryAccess();
  }

  /** 从存储中按 1 基索引拿出展示物品。 */
  public static void withdrawDisplayItem(ServerLevel level, ServerPlayer player, int index1) {
    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();
    if (index1 < 1 || index1 > list.size()) {
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[飞剑] 索引无效"));
      return;
    }
    FlyingSwordStorage.RecalledSword rec = list.get(index1 - 1);

    // 判定是否已取出
    if (rec.itemWithdrawn) {
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 该飞剑本体已取出，无法重复取出"));
      return;
    }

    // 构造展示物品堆
    ItemStack stack = ItemStack.EMPTY;
    // 1) 完整 NBT 恢复
    if (rec.displayItem != null && !rec.displayItem.isEmpty()) {
      stack = ItemStack.parseOptional(provider(level), rec.displayItem);
    }
    // 2) 回退到id
    if ((stack == null || stack.isEmpty()) && rec.displayItemId != null) {
      Item item = BuiltInRegistries.ITEM.get(rec.displayItemId);
      if (item != null && item != Items.AIR) {
        stack = new ItemStack(item);
      }
    }
    // 3) 再回退到模型默认
    if (stack == null || stack.isEmpty()) {
      ResourceLocation id = FlyingSwordModelTuning.defaultItemId();
      Item item = BuiltInRegistries.ITEM.get(id);
      stack = new ItemStack(item == null ? Items.IRON_SWORD : item);
    }

    // 将飞剑耐久比例映射到物品耐久（仅对可损耗物生效）
    try {
      if (rec.attributes != null) {
        double max = Math.max(1.0, rec.attributes.maxDurability);
        double percent = rec.durability / max; // 1=满耐久
        ItemDurabilityUtil.applyPercentToStack(stack, percent);
      }
    } catch (Throwable ignored) {}

    // 写入稳定UUID
    UUID uuid;
    Optional<UUID> fromRec = Optional.ofNullable(rec.displayItemUUID).flatMap(s -> {
      try { return Optional.of(UUID.fromString(s)); } catch (Exception e) { return Optional.empty(); }
    });
    if (fromRec.isPresent()) {
      uuid = fromRec.get();
    } else {
      uuid = ItemIdentityUtil.ensureItemUUID(stack);
    }
    // 确保物品本身也携带UUID（如前面来自NBT缺失的情况）
    if (rec.displayItemUUID == null || rec.displayItemUUID.isEmpty()) {
      rec.displayItemUUID = uuid.toString();
    } else {
      // 若物品未携带UUID，则写入
      var tagUuid = ItemIdentityUtil.getItemUUID(stack);
      if (tagUuid.isEmpty()) {
        ItemIdentityUtil.ensureItemUUID(stack);
      }
    }

    // 保存展示堆快照
    try {
      net.minecraft.nbt.Tag raw = stack.save(provider(level));
      rec.displayItem = raw instanceof CompoundTag ct ? ct.copy() : new CompoundTag();
      rec.displayItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
    } catch (Throwable ignored) {}

    // 发放给玩家（主手空则放主手，否则背包，否则掉落）
    boolean given = false;
    if (player.getMainHandItem().isEmpty()) {
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
      given = true;
    } else if (player.addItem(stack.copy())) {
      given = true;
    } else {
      player.drop(stack.copy(), false);
      given = true;
    }

    if (given) {
      rec.itemWithdrawn = true;
      String name = getStoredDisplayName(level, rec);
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              String.format(Locale.ROOT, "[飞剑] 已拿出：%s", name)));
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 提醒：存入/取出仅认UUID，请勿混用复制品"));
    }
  }

  /** 将主手物品放回存储。 */
  public static void depositMainHand(ServerLevel level, ServerPlayer player, int index1) {
    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();
    if (index1 < 1 || index1 > list.size()) {
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[飞剑] 索引无效"));
      return;
    }
    FlyingSwordStorage.RecalledSword rec = list.get(index1 - 1);

    // 判定是否已取出（只有取出状态才能放回）
    if (!rec.itemWithdrawn) {
      player.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 该飞剑本体未取出，无需放回"));
      return;
    }

    ItemStack hand = player.getMainHandItem();
    if (hand.isEmpty()) {
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[飞剑] 主手为空"));
      return;
    }
    // 更新存储快照
    net.minecraft.nbt.Tag raw = hand.save(provider(level));
    rec.displayItem = raw instanceof CompoundTag ct ? ct.copy() : new CompoundTag();
    rec.displayItemId = BuiltInRegistries.ITEM.getKey(hand.getItem());
    rec.itemWithdrawn = false;

    // 移除玩家手中1个
    hand.shrink(1);

    String name = getStoredDisplayName(level, rec);
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal(
            String.format(Locale.ROOT, "[飞剑] 已放回：%s", name)));
  }

  /** 计算存储项显示名称。 */
  public static String getStoredDisplayName(ServerLevel level, FlyingSwordStorage.RecalledSword rec) {
    try {
      if (rec.displayItem != null && !rec.displayItem.isEmpty()) {
        ItemStack stack = ItemStack.parseOptional(provider(level), rec.displayItem);
        if (!stack.isEmpty()) {
          return stack.getHoverName().getString();
        }
      }
      if (rec.displayItemId != null) {
        Item item = BuiltInRegistries.ITEM.get(rec.displayItemId);
        if (item != null && item != Items.AIR) {
          return new ItemStack(item).getHoverName().getString();
        }
      }
    } catch (Throwable ignored) {}
    return "飞剑";
  }

  /** 从存储中删除指定索引的飞剑（1基索引）。 */
  public static void deleteStoredSword(ServerLevel level, ServerPlayer player, int index1) {
    var storage = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);
    var list = storage.getRecalledSwords();
    if (index1 < 1 || index1 > list.size()) {
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[飞剑] 索引无效"));
      return;
    }

    // 直接删除，不做任何判定
    storage.remove(index1 - 1);
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal(
            String.format(Locale.ROOT, "[飞剑] 已删除第 %d 个飞剑", index1)));
  }
}
