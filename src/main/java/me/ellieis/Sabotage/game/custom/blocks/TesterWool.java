package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class TesterWool extends Block implements PolymerBlock {
    private final Block virtualBlock = Blocks.WOOL.white();
    public TesterWool(Properties settings) {
        super(settings);
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.virtualBlock.withPropertiesOf(state);
    }
}
