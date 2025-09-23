package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Lightweight accessors for Guzhenren mod items referenced by the compat layer.
 */
public final class GuzhenrenItems {

    private static final String MOD_ID = "guzhenren";

    public static final Item JIANJITENG = resolve("jianjiteng");
    public static final Item WEI_LIAN_HUA_JIAN_XIA_GU = resolve("weilianhuajianxiagu");
    public static final Item WEI_LIAN_HUA_JIAN_ZHI_GU_3 = resolve("wei_lian_hua_jian_zhi_gu_3");
    public static final Item WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU = resolve("weilianhuajinwenjianxiagu");
    public static final Item WEI_LIAN_HUA_JIN_HEN_GU = resolve("weilianhuajinhengu");
    public static final Item JIANMAI_GU_GUFANG = resolve("jianmaigugufang");

    private static final Item[] JIANDAO_BONUS_ITEMS = new Item[] {
            WEI_LIAN_HUA_JIAN_XIA_GU,
            WEI_LIAN_HUA_JIAN_ZHI_GU_3,
            WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU,
            WEI_LIAN_HUA_JIN_HEN_GU,
            JIANMAI_GU_GUFANG
    };

    private GuzhenrenItems() {
    }

    private static Item resolve(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
        return item == null ? Items.AIR : item;
    }

    public static Item pickRandomJiandaoBonus(RandomSource random) {
        if (random == null || JIANDAO_BONUS_ITEMS.length == 0) {
            return JIANMAI_GU_GUFANG;
        }
        int index = random.nextInt(JIANDAO_BONUS_ITEMS.length);
        Item selected = JIANDAO_BONUS_ITEMS[index];
        return selected == Items.AIR ? JIANMAI_GU_GUFANG : selected;
    }
}
