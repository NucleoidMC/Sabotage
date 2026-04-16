package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import xyz.nucleoid.packettweaker.PacketContext;

public class TesterWool extends Block implements PolymerBlock {
    private final Block virtualBlock = Blocks.WHITE_WOOL;
    public TesterWool(Properties settings) {
        super(settings);
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.virtualBlock.withPropertiesOf(state);
    }
}
