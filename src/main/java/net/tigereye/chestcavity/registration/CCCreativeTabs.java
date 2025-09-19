package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;

public class CCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChestCavity.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORGAN_TAB = TABS.register("organs", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + ChestCavity.MODID + ".organs"))
            .icon(() -> new ItemStack(CCItems.HUMAN_STOMACH.get()))
            .displayItems((parameters, output) -> CCItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());
}
