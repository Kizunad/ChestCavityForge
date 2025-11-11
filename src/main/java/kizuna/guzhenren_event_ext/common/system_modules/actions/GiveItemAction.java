package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Action：给予玩家指定物品
 * <p>
 * JSON 配置格式：
 * <pre>
 * {
 *   "type": "guzhenren_event_ext:give_item",
 *   "item": "minecraft:diamond",        // 必需：物品ID（ResourceLocation格式）
 *   "count": 5,                         // 可选：数量，默认1
 *   "nbt": "{Enchantments:[...]}"       // 可选：NBT数据（JSON字符串格式）
 * }
 * </pre>
 * <p>
 * 示例：
 * <pre>
 * {
 *   "type": "guzhenren_event_ext:give_item",
 *   "item": "guzhenren:xian_yuan_shi",
 *   "count": 10
 * }
 * </pre>
 */
public class GiveItemAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        // 1. 参数校验
        if (!definition.has("item")) {
            GuzhenrenEventExtension.LOGGER.warn("GiveItemAction 缺少 'item' 字段");
            return;
        }

        // 2. 解析物品ID
        String itemIdStr = definition.get("item").getAsString();
        ResourceLocation itemId;
        try {
            itemId = ResourceLocation.parse(itemIdStr);
        } catch (Exception e) {
            GuzhenrenEventExtension.LOGGER.error("GiveItemAction 无效的物品ID: {}", itemIdStr, e);
            return;
        }

        // 3. 获取物品
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            GuzhenrenEventExtension.LOGGER.error("GiveItemAction 找不到物品: {}", itemId);
            return;
        }

        // 4. 解析数量
        int count = definition.has("count") ? definition.get("count").getAsInt() : 1;
        count = Math.max(1, Math.min(count, 64)); // 限制在 [1, 64]

        // 5. 创建 ItemStack
        ItemStack itemStack = new ItemStack(item, count);

        // 6. 可选：应用 NBT 数据
        if (definition.has("nbt")) {
            try {
                String nbtStr = definition.get("nbt").getAsString();
                CompoundTag nbt = TagParser.parseTag(nbtStr);

                // 注意：在现代 Minecraft 版本中，应该使用 DataComponentPatch 而不是直接设置 NBT
                // 这里提供一个基础实现，根据需要可以扩展为 DataComponent 支持
                // itemStack.setTag(nbt); // 旧版本方式

                GuzhenrenEventExtension.LOGGER.debug("GiveItemAction NBT 支持需要根据具体 Minecraft 版本实现");
                // TODO: 如果需要 NBT 支持，请根据 NeoForge 版本实现 DataComponentPatch 应用
            } catch (Exception e) {
                GuzhenrenEventExtension.LOGGER.error("GiveItemAction 解析 NBT 失败", e);
            }
        }

        // 7. 给予玩家物品
        boolean success = player.getInventory().add(itemStack);

        if (!success) {
            // 背包满了，掉落在地上
            player.drop(itemStack, false);
            GuzhenrenEventExtension.LOGGER.debug(
                "GiveItemAction: 玩家 {} 背包已满，物品 {} x{} 已掉落",
                player.getName().getString(), itemId, count
            );
        } else {
            GuzhenrenEventExtension.LOGGER.debug(
                "GiveItemAction: 给予玩家 {} 物品 {} x{}",
                player.getName().getString(), itemId, count
            );
        }
    }
}
