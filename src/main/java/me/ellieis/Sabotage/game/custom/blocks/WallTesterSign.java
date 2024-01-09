package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WallTesterSign extends WallSignBlock implements PolymerBlock {
    private final Block virtualBlock;
    public WallTesterSign(Settings settings, Block virtualBlock) {
        super(WoodType.OAK, settings);
        this.virtualBlock = virtualBlock;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return this.virtualBlock;
    }
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient()) {
            ServerPlayerEntity plr = (ServerPlayerEntity) player;
            for (SabotageActive game : Sabotage.activeGames) {
                if (game.getWorld().equals(world)) {
                    if (!game.testEntity(plr, hit.getBlockPos())) {
                        plr.sendMessage(Text.translatable("sabotage.tester.fail").formatted(Formatting.YELLOW));
                    }
                    break;
                }
            }
        }
        return ActionResult.FAIL;
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return this.virtualBlock.getStateWithProperties(state);
    }

    @Override
    public void openEditScreen(PlayerEntity player, SignBlockEntity blockEntity, boolean front) {
        // we don't want players to edit this sign, so this is just a noop.
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient()) {
            SignBlockEntity be = (SignBlockEntity) world.getBlockEntity(pos);
            Text[] text = {Text.literal("Click this"), Text.literal("sign to"), Text.literal("start the"), Text.literal("test")};
            be.setText(new SignText(text, text, DyeColor.RED, true), true);
        }
    }
}
