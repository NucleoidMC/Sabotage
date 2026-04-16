package me.ellieis.Sabotage.game.custom.blocks;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.core.BlockPos;

import static me.ellieis.Sabotage.game.custom.SabotageBlocks.TESTER_SIGN_ENTITY;

public class TesterSignBlockEntity extends SignBlockEntity {
    public TesterSignBlockEntity(BlockPos pos, BlockState state) {
        super(TESTER_SIGN_ENTITY, pos, state);
    }
}
