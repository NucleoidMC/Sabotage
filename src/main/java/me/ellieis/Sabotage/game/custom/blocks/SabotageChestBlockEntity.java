package me.ellieis.Sabotage.game.custom.blocks;

import me.ellieis.Sabotage.game.custom.SabotageBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.core.BlockPos;

public class SabotageChestBlockEntity extends ChestBlockEntity {

    public SabotageChestBlockEntity(BlockPos pos, BlockState state) {
        super(SabotageBlocks.SABOTAGE_CHEST_ENTITY, pos, state);
    }
}
