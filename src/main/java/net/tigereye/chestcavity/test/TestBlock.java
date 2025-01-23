package net.tigereye.chestcavity.test;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class TestBlock extends Block implements EntityBlock {
    public TestBlock(Properties p_49795_) {
        super(p_49795_);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;//BlockEntityRegistry.TEST.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos pos, Player player_, InteractionHand p_60507_, BlockHitResult p_60508_) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof TestBlockEntity entity) {
            //open screen
            final MenuProvider container = new SimpleMenuProvider((id, playerInv, player) -> new TestContainer(id, playerInv, entity.inventory, pos), new TextComponent("idk"));
            NetworkHooks.openGui((ServerPlayer) player_, container, pos);
        }

        return InteractionResult.CONSUME;
    }
}
