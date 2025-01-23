package net.tigereye.chestcavity.test;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;


@Mod.EventBusSubscriber(modid = ChestCavity.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> DEF = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, ChestCavity.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ChestCavity.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, ChestCavity.MODID);

    //public static final RegistryObject<MenuType<TestContainer>> CONT = MENU_TYPES.register("test", () -> new MenuType<>(TestContainer::new));
    //public static final RegistryObject<BlockEntityType<?>> TEST = DEF.register("test", () -> BlockEntityType.Builder.of(TestBlockEntity::new).build(null));
    //public static final RegistryObject<Block> BLOCK = BLOCKS.register("test", () -> new TestBlock(BlockBehaviour.Properties.of(Material.METAL)));

    public static final RegistryObject<MenuType<ExampleChestContainer>> EXAMPLE_CHEST_MENU = MENU_TYPES.register("test", () -> new MenuType<>(ExampleChestContainer::new));
    public static final RegistryObject<BlockEntityType<?>> EXAMPLE_CHEST_BE = DEF.register("test", () -> BlockEntityType.Builder.of(ExampleChestBlockEntity::new).build(null));
    public static final RegistryObject<Block> EXAMPLE_CHEST_BLOCK = BLOCKS.register("test", () -> new ExampleChestBlock(BlockBehaviour.Properties.of(Material.METAL)));


    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        //event.getRegistry().register(new BlockItem(BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)).setRegistryName(ChestCavity.MODID, "test"));
        event.getRegistry().register(new BlockItem(EXAMPLE_CHEST_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)).setRegistryName(ChestCavity.MODID, "test"));
    }

    public static void registerBus(IEventBus bus) {
        DEF.register(bus);
        BLOCKS.register(bus);
        MENU_TYPES.register(bus);
    }
}
