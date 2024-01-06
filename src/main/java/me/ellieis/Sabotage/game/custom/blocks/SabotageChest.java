package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SabotageChest extends Block implements BlockEntityProvider, PolymerBlock {
    private final Block virtualBlock;

    public SabotageChest(Settings settings, Block virtualBlock) {
        super(settings);

        this.virtualBlock = virtualBlock;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return this.virtualBlock;
    }
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SabotageChestBlockEntity(pos, state);
    }
}
