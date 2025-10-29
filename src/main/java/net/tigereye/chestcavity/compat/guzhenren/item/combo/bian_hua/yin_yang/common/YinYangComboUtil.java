package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.common;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/**
 * 阴阳转身蛊组合杀招的通用工具
 */
public final class YinYangComboUtil {

    public static final ResourceLocation YIN_YANG_GU_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "yin_yang_zhuan_shen_gu");
    private static final String ORGAN_STATE_ROOT = "YinYangZhuanShenGu";

    private YinYangComboUtil() {}

    /**
     * Finds the Yin-Yang Gu organ in the player's chest cavity.
     * @param cc The chest cavity instance.
     * @return An optional containing the ItemStack of the organ if found, otherwise empty.
     */
    public static Optional<ItemStack> findYinYangGu(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(YIN_YANG_GU_ID);
        if (item == null) {
            return Optional.empty();
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the OrganState for the Yin-Yang Gu.
     * @param organ The ItemStack of the Yin-Yang Gu.
     * @return The OrganState instance.
     */
    public static OrganState resolveState(ItemStack organ) {
        return OrganState.of(organ, ORGAN_STATE_ROOT);
    }

    /**
     * Gets a MultiCooldown instance for the Yin-Yang Gu.
     * @param cc The chest cavity instance.
     * @param organ The ItemStack of the Yin-Yang Gu.
     * @param state The OrganState of the Yin-Yang Gu.
     * @return A MultiCooldown instance.
     */
    public static MultiCooldown getCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        return MultiCooldown.builder(state).withSync(cc, organ).build();
    }
}
