package me.ellieis.Sabotage.game.custom.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.math.BlockPos;

import static me.ellieis.Sabotage.game.custom.SabotageBlocks.TESTER_SIGN_ENTITY;

public class TesterSignBlockEntity extends SignBlockEntity {
    public TesterSignBlockEntity(BlockPos pos, BlockState state) {
        super(TESTER_SIGN_ENTITY, pos, state);
    }
}
