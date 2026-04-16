package me.ellieis.Sabotage.game.custom.blocks;

import com.google.common.util.concurrent.AtomicDouble;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.GameStates;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.statistics.GlobalPlayerStatistics;
import me.ellieis.Sabotage.game.statistics.SabotagePlayerStatistics;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Map.entry;
import static me.ellieis.Sabotage.game.custom.SabotageBlocks.SABOTAGE_CHEST_ENTITY;

public class SabotageChest extends ChestBlock implements EntityBlock, PolymerBlock {
    private final Block virtualBlock = Blocks.CHEST;
    private static final Map<Item, Integer> items = Map.ofEntries(
            entry(Items.WOODEN_SWORD, 40),
            entry(Items.STONE_SWORD, 15),
            entry(Items.IRON_SWORD, 5),
            entry(Items.WOODEN_AXE, 5),
            entry(Items.GOLDEN_AXE, 1),
            entry(Items.LEATHER_BOOTS, 20),
            entry(Items.LEATHER_HELMET, 20),
            entry(Items.FIREWORK_ROCKET, 10),
            entry(Items.ARROW, 20),
            entry(Items.LEATHER_LEGGINGS, 12),
            entry(Items.LEATHER_CHESTPLATE, 10),
            entry(Items.CHAINMAIL_BOOTS, 8),
            entry(Items.CHAINMAIL_HELMET, 8),
            entry(Items.CHAINMAIL_LEGGINGS, 5),
            entry(Items.CHAINMAIL_CHESTPLATE, 3),
            entry(Items.IRON_HELMET, 5),
            entry(Items.IRON_BOOTS, 5),
            entry(Items.BOW, 4),
            entry(Items.CROSSBOW, 4),
            entry(Items.IRON_LEGGINGS, 3),
            entry(Items.TNT, 3),
            entry(Items.GOLDEN_APPLE, 3),
            entry(Items.IRON_CHESTPLATE, 1)

    );
    private static final Map<Item, Integer> durabilities = Map.ofEntries(
            entry(Items.GOLDEN_AXE, 3),
            entry(Items.IRON_SWORD, 50),
            entry(Items.WOODEN_AXE, 5),
            entry(Items.IRON_CHESTPLATE, 50),
            entry(Items.IRON_LEGGINGS, 50),
            entry(Items.BOW, 100),
            entry(Items.CROSSBOW, 30)
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
        ItemStack stack = new ItemStack(item);
        if (item == Items.FIREWORK_ROCKET) {
            IntList colors = IntList.of(DyeColor.ORANGE.getFireworkColor());
            FireworkExplosion explode = new FireworkExplosion(FireworkExplosion.Shape.BURST, colors, IntList.of(), false, false);
            Fireworks fireworkComponent = new Fireworks(0, List.of(explode));
            stack.set(DataComponents.FIREWORKS, fireworkComponent);
        } else if (item == Items.ARROW) {
            stack.setCount(8);
        }

        if (durabilities.get(item) != null) {
            int range = durabilities.get(item);
            int durability = (int) Math.floor(Math.random() * range);
            stack.setDamageValue(stack.getMaxDamage() - durability);
        }
        return stack;
    }
    public SabotageChest(Properties settings) {
        super(() -> SABOTAGE_CHEST_ENTITY, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, settings);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level woorld, BlockPos pos, Player plr, BlockHitResult hit) {
        if (woorld.isClientSide()) return InteractionResult.PASS;
        SabotageActive game = null;

        for (SabotageActive activeGame : Sabotage.activeGames) {
            if (activeGame.getWorld().equals(woorld)) {
                game = activeGame;
                break;
            }
        }
        if (game != null && game.gameState != GameStates.COUNTDOWN) {
            ServerLevel world = (ServerLevel) woorld;
            world.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1, 1.2f);
            Vec3 center = pos.getCenter();
            world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y, center.z, 16, 0, 0, 0, 0.5);

            Inventory inventory = plr.getInventory();
            ItemStack item = getItemDrop();
            if (!inventory.add(item)) {
                // couldn't insert stack, inventory is likely full
                world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), item));
            }

            game.stats.forPlayer((ServerPlayer) plr).increment(SabotagePlayerStatistics.CHESTS_OPENED, 1);
            game.stats.global().increment(GlobalPlayerStatistics.TOTAL_CHESTS_OPENED, 1);

            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            world.updateNeighborsAt(pos, Blocks.AIR);
        }
        return InteractionResult.FAIL;
    }
    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.virtualBlock.withPropertiesOf(state);
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SabotageChestBlockEntity(pos, state);
    }
    @Override
    public void onPolymerBlockSend(BlockState blockState, BlockPos.MutableBlockPos pos, ServerPlayer plr) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
        nbt.putString("id", "minecraft:chest");
        plr.connection.send(PolymerBlockUtils.createBlockEntityPacket(pos, BlockEntityType.CHEST, nbt));
    }
}
