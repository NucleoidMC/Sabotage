package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class WallTesterSign extends WallSignBlock implements PolymerBlock {
    private final Block virtualBlock = Blocks.OAK_SIGN;

    public WallTesterSign(Properties settings) {
        super(WoodType.OAK, settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        BaseTesterSign.onPlaced(world, pos);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BaseTesterSign.onUse(world, player, pos);
        return InteractionResult.FAIL;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TesterSignBlockEntity(pos, state);
    }
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.virtualBlock.withPropertiesOf(state);
    }

    @Override
    public void openTextEdit(Player player, SignBlockEntity blockEntity, boolean front) {
        // we don't want players to edit this sign, so this is just a noop.
    }

}
