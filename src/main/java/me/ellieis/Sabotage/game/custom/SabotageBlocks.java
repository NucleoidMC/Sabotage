package me.ellieis.Sabotage.game.custom;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.custom.blocks.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import java.util.function.Function;


public class SabotageBlocks {
    public static final SabotageChest SABOTAGE_CHEST = register("sabotage_chest", SabotageChest::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noLootTable());
    public static final TesterWool TESTER_WOOL = register("tester_wool", TesterWool::new, BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.white()).noLootTable());
    public static final TesterSign TESTER_SIGN = register("tester_sign", TesterSign::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN).noLootTable());
    public static final WallTesterSign WALL_TESTER_SIGN = register("wall_tester_sign", WallTesterSign::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).noLootTable());

    public static final BlockEntityType<TesterSignBlockEntity> TESTER_SIGN_ENTITY = registerBlockEntity("tester_sign_block_entity", FabricBlockEntityTypeBuilder.create(TesterSignBlockEntity::new, TESTER_SIGN, WALL_TESTER_SIGN).build());
    public static final BlockEntityType<SabotageChestBlockEntity> SABOTAGE_CHEST_ENTITY = registerBlockEntity("sabotage_chest_block_entity", FabricBlockEntityTypeBuilder.create(SabotageChestBlockEntity::new, SABOTAGE_CHEST).build());

    private static <T extends BlockBehaviour> T register(String id, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties settings) {
        Block block = (Block) factory.apply(settings.setId(ResourceKey.create(Registries.BLOCK, Sabotage.identifier(id))));
        return (T) Registry.register(BuiltInRegistries.BLOCK, Sabotage.identifier(id), block);
    }
    private static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(String id, BlockEntityType<T> type) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Sabotage.identifier(id), type);
        PolymerBlockUtils.registerBlockEntity(type);
        return type;
    }
    public static void register() {}
}
