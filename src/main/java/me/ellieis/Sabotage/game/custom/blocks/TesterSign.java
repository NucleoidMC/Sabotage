package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class TesterSign extends SignBlock implements PolymerBlock {
    private final Block virtualBlock = Blocks.OAK_SIGN;

    public TesterSign(Settings settings) {
        super(WoodType.OAK, settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        BaseTesterSign.onPlaced(world, pos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BaseTesterSign.onUse(world, player, pos);
        return ActionResult.FAIL;
    }

    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TesterSignBlockEntity(pos, state);
    }
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.virtualBlock.getStateWithProperties(state);
    }

    @Override
    public void openEditScreen(PlayerEntity player, SignBlockEntity blockEntity, boolean front) {
        // we don't want players to edit this sign, so this is just a noop.
    }

    @Override
    public void onPolymerBlockSend(BlockState blockState, BlockPos.Mutable pos, PacketContext.NotNullWithPlayer context) {
        context.getPlayer().networkHandler.sendPacket(BaseTesterSign.getBlockEntityPacket(pos));
    }
}
