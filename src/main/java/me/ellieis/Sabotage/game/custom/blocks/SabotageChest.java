package me.ellieis.Sabotage.game.custom.blocks;

import com.google.common.util.concurrent.AtomicDouble;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.GameStates;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.statistics.GlobalPlayerStatistics;
import me.ellieis.Sabotage.game.statistics.SabotagePlayerStatistics;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Map.entry;
import static me.ellieis.Sabotage.game.custom.SabotageBlocks.SABOTAGE_CHEST_ENTITY;

public class SabotageChest extends ChestBlock implements BlockEntityProvider, PolymerBlock {
    private final Block virtualBlock;
    private static final Map<Item, Integer> items = Map.ofEntries(
            entry(Items.WOODEN_SWORD, 40),
            entry(Items.STONE_SWORD, 15),
            entry(Items.IRON_SWORD, 5),
            entry(Items.WOODEN_AXE, 5),
            entry(Items.GOLDEN_AXE, 1),
            entry(Items.LEATHER_BOOTS, 20),
            entry(Items.LEATHER_HELMET, 20),
            entry(Items.LEATHER_LEGGINGS, 12),
            entry(Items.LEATHER_CHESTPLATE, 10),
            entry(Items.CHAINMAIL_BOOTS, 8),
            entry(Items.CHAINMAIL_HELMET, 8),
            entry(Items.CHAINMAIL_LEGGINGS, 5),
            entry(Items.CHAINMAIL_CHESTPLATE, 3),
            entry(Items.IRON_HELMET, 5),
            entry(Items.IRON_BOOTS, 5),
            entry(Items.IRON_LEGGINGS, 3),
            entry(Items.IRON_CHESTPLATE, 1),
            entry(Items.GOLDEN_APPLE, 1),
            entry(Items.BOW, 4),
            entry(Items.CROSSBOW, 4),
            entry(Items.FIREWORK_ROCKET, 10),
            entry(Items.ARROW, 20)
    );
    @FunctionalInterface
    private interface ThreadLocalRandomWrapper {
        ThreadLocalRandom current();
    }
    // I stole this from stackoverflow, don't ask me how it works
    private static <T> T getFromWeightedMap(Map<T, ? extends Number> weights) {
        ThreadLocalRandomWrapper THREAD_LOCAL = ThreadLocalRandom::current;
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double chance = THREAD_LOCAL.current().nextDouble() * weights.values().stream().map(Number::doubleValue).reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue().doubleValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }
    private static ItemStack getItemDrop() {
        Item item = getFromWeightedMap(items);
        return new ItemStack(item);
    }
    public SabotageChest(Settings settings, Block virtualBlock) {
        super(settings, () -> SABOTAGE_CHEST_ENTITY);

        this.virtualBlock = virtualBlock;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return this.virtualBlock;
    }

    @Override
    public ActionResult onUse(BlockState state, World woorld, BlockPos pos, PlayerEntity plr, Hand hand, BlockHitResult hit) {
        if (woorld.isClient()) return ActionResult.PASS;
        SabotageActive game = null;

        for (SabotageActive activeGame : Sabotage.activeGames) {
            if (activeGame.getWorld().equals(woorld)) {
                game = activeGame;
                break;
            }
        }
        if (game != null && game.gameState != GameStates.COUNTDOWN) {
            ServerWorld world = (ServerWorld) woorld;
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1, 1.2f);
            PlayerInventory inventory = plr.getInventory();
            ItemStack item = getItemDrop();
            if (!inventory.insertStack(item)) {
                // couldn't insert stack, inventory is likely full
                world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), item));
            }
            game.stats.forPlayer((ServerPlayerEntity) plr).increment(SabotagePlayerStatistics.CHESTS_OPENED, 1);
            game.stats.global().increment(GlobalPlayerStatistics.TOTAL_CHESTS_OPENED, 1);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
        return ActionResult.FAIL;
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return this.virtualBlock.getStateWithProperties(state);
    }

    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SabotageChestBlockEntity(pos, state);
    }
}
