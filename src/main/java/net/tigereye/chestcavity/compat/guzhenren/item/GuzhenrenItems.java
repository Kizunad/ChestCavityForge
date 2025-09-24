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
    public static final Item CHOU_PI_GU = resolve("chou_pi_gu");
    public static final Item TIE_XUE_GU = resolve("tiexuegu");

    public static final Item XUE_FEI_GU = resolve("xue_fei_gu");

    public static final Item XIE_DI_GU = resolve("xie_di_gu");


    public static final Item WEI_LIAN_HUA_JIAN_XIA_GU = resolve("weilianhuajianxiagu");
    public static final Item WEI_LIAN_HUA_JIAN_ZHI_GU_3 = resolve("wei_lian_hua_jian_zhi_gu_3");
    public static final Item WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU = resolve("weilianhuajinwenjianxiagu");
    public static final Item WEI_LIAN_HUA_JIN_HEN_GU = resolve("weilianhuajinhengu");
    public static final Item WEI_LIAN_HUA_JIAN_MAI_GU = resolve("weilianhuajianmaigu");

    private static final Item[] JIANDAO_BONUS_ITEMS = new Item[] {
            WEI_LIAN_HUA_JIAN_XIA_GU,
            WEI_LIAN_HUA_JIAN_ZHI_GU_3,
            WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU,
            WEI_LIAN_HUA_JIN_HEN_GU,
            WEI_LIAN_HUA_JIAN_MAI_GU
    };

    private GuzhenrenItems() {
    }

    private static Item resolve(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
        return item == null ? Items.AIR : item;
    }

    public static Item pickRandomJiandaoBonus(RandomSource random) {
        if (random == null || JIANDAO_BONUS_ITEMS.length == 0) {
            return WEI_LIAN_HUA_JIAN_MAI_GU;
        }
        int index = random.nextInt(JIANDAO_BONUS_ITEMS.length);
        Item selected = JIANDAO_BONUS_ITEMS[index];
        return selected == Items.AIR ? WEI_LIAN_HUA_JIAN_MAI_GU : selected;

    }
}
