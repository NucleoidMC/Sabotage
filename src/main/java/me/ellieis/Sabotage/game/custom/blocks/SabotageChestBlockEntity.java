package me.ellieis.Sabotage.game.custom.blocks;

import me.ellieis.Sabotage.game.custom.SabotageBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;

public class SabotageChestBlockEntity extends ChestBlockEntity {

    public SabotageChestBlockEntity(BlockPos pos, BlockState state) {
        super(SabotageBlocks.SABOTAGE_CHEST_ENTITY, pos, state);
    }
}
